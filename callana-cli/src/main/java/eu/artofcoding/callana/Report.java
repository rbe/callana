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

    private static class EvnData implements Comparable {

        private String ARufnummer;

        private String einwahlRufnummer;

        private Date datum;

        private int dauerInSekunden;

        public String getARufnummer() {
            return ARufnummer;
        }

        public void setARufnummer(String ARufnummer) {
            this.ARufnummer = ARufnummer;
        }

        public String getEinwahlRufnummer() {
            return einwahlRufnummer;
        }

        public void setEinwahlRufnummer(String einwahlRufnummer) {
            this.einwahlRufnummer = einwahlRufnummer;
        }

        public Date getDatum() {
            return datum;
        }

        public void setDatum(Date datum) {
            this.datum = datum;
        }

        public int getDauerInSekunden() {
            return dauerInSekunden;
        }

        public void setDauerInSekunden(int dauerInSekunden) {
            this.dauerInSekunden = dauerInSekunden;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof EvnData) {
                EvnData e = (EvnData) o;
                int sort1 = this.ARufnummer.compareTo(e.ARufnummer);
                //int sort2 = this.datum.compareTo(e.datum);
                return sort1;
            } else {
                return 0;
            }
        }

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
        System.out.println("usage: java -jar Callana.jar <inputfile> <Odisee user> <Odisee password>");
        System.out.println("No input file specified");
    }

    public static void main(String[] args) throws IOException {
        // Input: EVN
        if (args.length == 3) {
            String inputFilename = args[0];
            String odiseeUsername = args[1];
            String odiseePassword = args[2];
            Path evnInput = Paths.get(inputFilename).normalize();
            analyse(evnInput);
            // Create document
            OdiseeClient client = OdiseeClient.createClient("http://service.odisee.de/odisee/document/generate", odiseeUsername, odiseePassword);
            client.createRequest("EVN").setArchive(false, true);
            client.setUserfield("SchreibenDatum", sdfGermanDate.format(new Date())).
                    setUserfield("Kunde", "Acme, Inc.").
                    setUserfield("Anrede", "Herr").
                    setUserfield("Vorname", "Max").
                    setUserfield("Nachname", "Mustermann").
                    setUserfield("Anschrift", "Abcstrasse 1").
                    setUserfield("PLZ", "12345").
                    setUserfield("Ort", "Ort").
                    setUserfield("Betreff", "Einzelverbindungsnachweis");
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
