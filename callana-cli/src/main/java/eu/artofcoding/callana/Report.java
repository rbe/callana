/*
 * callana
 * callana
 * Copyright (C) 2013-2013 art of coding UG, http://www.art-of-coding.eu/
 *
 * Alle Rechte vorbehalten. Nutzung unterliegt Lizenzbedingungen.
 * All rights reserved. Use is subject to license terms.
 *
 * rbe, 14.01.13 15:34
 */

package eu.artofcoding.callana;

import eu.artofcoding.beetlejuice.helper.StreamHelper;
import eu.artofcoding.odisee.client.OdiseeClient;
import eu.artofcoding.odisee.client.OdiseeClientException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Report {

    /**
     * Date parsing and formatting.
     */
    private static SimpleDateFormat sdfGermanDate = new SimpleDateFormat("dd.MM.yyyy");

    private static SimpleDateFormat sdfGermanDateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd", Locale.GERMANY);

    private static SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.GERMANY);

    /**
     * Currency formatting.
     */
    private static NumberFormat eurGerman = DecimalFormat.getCurrencyInstance(Locale.GERMANY);

    private static Date fromDate = null;

    private static Date toDate = null;

    private static NumberFormat numberGerman = DecimalFormat.getNumberInstance(Locale.GERMANY);

    /**
     * Report data structure.
     */
    private static List<EvnData> evnData = new ArrayList<>();

    static {
        eurGerman.setMinimumFractionDigits(2);
        eurGerman.setMaximumFractionDigits(2);
        eurGerman.setGroupingUsed(true);
        numberGerman.setMinimumFractionDigits(0);
        numberGerman.setMaximumFractionDigits(0);
        numberGerman.setGroupingUsed(true);
    }

    private static void analyse(Path evn) throws IOException {
        //Path odiseeXml = Paths.get(evn.);
        // Read EVN
        // DS     4052630455  019282033           20121214005319000023+0000000000VER
        //        A-Rufnummer Einwahlnr.          yyyyMMddHHMMSSDAUER-
        //                                        01234567890123456789
        Scanner scanner = new Scanner(evn);
        // Read every line
        int lineCount = 0;
        EvnData temp = null;
        String datumUndDauer;
        Date d;
        int dauerTotal = 0;
        while (scanner.hasNextLine()) {
            scanner.nextLine();
            if (scanner.hasNext("VS")) {
                // Skip first line
            } else if (scanner.hasNext("DS")) {
                scanner.next("DS");
                lineCount++;
                // Create new EvnData bean
                temp = new EvnData();
                temp.setARufnummer(String.format("+49%s", scanner.next()));
                temp.setEinwahlRufnummer(scanner.next());
                // Split and analyze string like 20121214005319000023+0000000000VER into yyyymmddHHMMSSDAUER-REST
                datumUndDauer = scanner.next();
                String[] splitByPlusSign = datumUndDauer.split("\\+");
                String datum = splitByPlusSign[0].substring(0, 8);
                String zeit = splitByPlusSign[0].substring(8, 14);
                String dauer = splitByPlusSign[0].substring(14);
                try {
                    d = sdfDateTime.parse(String.format("%s %s", datum, zeit));
                    temp.setDatum(d);
                    temp.setDauerInSekunden(Integer.valueOf(dauer));
                    dauerTotal += temp.getDauerInSekunden();
                    System.out.printf("Rufnummer=%s Datum=%s %s -> %s dauerTotal=%d%n", temp.getARufnummer(), datum, zeit, d.toString(), dauerTotal);
                    evnData.add(temp);
                } catch (ParseException e) {
                    System.out.printf("Cannot parse, exception: %s%n", e.getMessage());
                }
            } else if (scanner.hasNext("SS\\d.*")) {
                String lastLine = scanner.next("SS\\d.*");
                String[] split = lastLine.split("\\+");
                //int statLineCount = Integer.valueOf(split[0].substring(2));
                // 000000000000002012121020121216
                // 012345678901234567890123456789
                String timerange = split[1];
                try {
                    fromDate = sdfDate.parse(timerange.substring(14, 22));
                    toDate = sdfDate.parse(timerange.substring(22));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void usage() {
        System.out.printf("usage: %s <inputfile>%n", Report.class.getName());
        System.out.println("No input file specified");
    }

    public static void main(String[] args) throws IOException {
        // Input: EVN
        if (args.length == 1) {
            String inputFilename = args[0];
            Path evnInput = Paths.get(inputFilename).normalize();
            analyse(evnInput);
            // Properties
            Properties props = new Properties();
            props.load(Report.class.getResourceAsStream("callana.properties"));
            String odiseeUsername = (String) props.get("callana.odisee.user");
            String odiseePassword = (String) props.get("callana.odisee.password");
            // Create document
            OdiseeClient client = OdiseeClient.createClient("http://service.odisee.de/odisee/document/generate", odiseeUsername, odiseePassword);
            client.createRequest("EVN");
            client.setArchive(false, true);
            client.setUserfield("SchreibenDatum", sdfGermanDate.format(new Date())).
                    setUserfield("Kunde", (String) props.get("callana.kunde")).
                    setUserfield("Anrede", (String) props.get("callana.anrede")).
                    setUserfield("Vorname", (String) props.get("callana.vorname")).
                    setUserfield("Nachname", (String) props.get("callana.nachname")).
                    setUserfield("Anschrift", (String) props.get("callana.anschrift")).
                    setUserfield("PLZ", (String) props.get("callana.plz")).
                    setUserfield("Ort", (String) props.get("callana.ort")).
                    setUserfield("Betreff", (String) props.get("callana.betreff"));
            if (null != fromDate && null != toDate) {
                client.setUserfield("Zeitraum", String.format("%s bis %s", sdfGermanDate.format(fromDate), sdfGermanDate.format(toDate)));
            }
            // Sort
            Collections.sort(evnData);
            //
            int line = 0;
            int dauerTotal = 0;
            for (; line < evnData.size(); line++) {
                EvnData e = evnData.get(line);
                dauerTotal += e.getDauerInSekunden();
                client.setTableCellValue("EVNPositionen", String.format("A%d", line + 3), e.getARufnummer());
                client.setTableCellValue("EVNPositionen", String.format("B%d", line + 3), e.getEinwahlRufnummer());
                client.setTableCellValue("EVNPositionen", String.format("C%d", line + 3), String.format("%s Uhr", sdfGermanDateTime.format(e.getDatum())));
                client.setTableCellValue("EVNPositionen", String.format("D%d", line + 3), String.format("%d Sekunden", e.getDauerInSekunden()));
                client.setTableCellValue("EVNPositionen", String.format("E%d", line + 3), eurGerman.format(0.01));
                client.setTableCellValue("EVNPositionen", String.format("F%d", line + 3), eurGerman.format(e.getDauerInSekunden() * 0.01));
            }
            client.setTableCellValue("EVNSumme", "B1", String.format("%s Sekunden", numberGerman.format(dauerTotal)));
            // Dauer in Minuten * 0,01 €
            client.setTableCellValue("EVNSumme", "C1", eurGerman.format(dauerTotal / 60 * 0.01));
            // Call Odisee
            try {
                byte[] document = client.process();
                StreamHelper.saveToFile(document, new File(String.format("%s.pdf", inputFilename.replace('.', '_'))));
            } catch (OdiseeClientException e) {
                e.printStackTrace();
            }
        } else {
            usage();
        }
    }

}
