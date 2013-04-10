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

import java.util.Date;

public class EvnData implements Comparable {

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
