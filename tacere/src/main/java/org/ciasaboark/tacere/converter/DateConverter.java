/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.converter;

public class DateConverter {
    private int numberOfDays;

    public DateConverter(int days) {
        numberOfDays = days;
    }

    public int getDays() {
        return numberOfDays;
    }

    public int getWeeks() {
        int weeks =  numberOfDays / 7;
        return weeks;
    }

    public int getMonths() {
        int months =  numberOfDays / 30;   //generic 30 day month
        return months;
    }

    public int getYears() {
        int years =  numberOfDays / 365;  //leap years are a lie
        return years;
    }

    public String toString() {
        int days = numberOfDays;
        int years = getYears();
        if (years != 0) {
            days = days % 365;
        }

        int months = getMonths();
        if (months != 0) {
            days = days % 30;
        }

        int weeks = getWeeks();
        if (weeks != 0) {
            days = days % 7;
        }

        String formattedString = "";
        if (years != 0) {
            if (years == 1) {
                formattedString += years + " year ";
            } else {
                formattedString += years + " years ";
            }
        }
        if (months != 0) {
            if (months == 1) {
                formattedString += months + " month ";
            } else {
                formattedString += months + " months ";
            }
        }
        if (weeks != 0) {
            if (weeks == 1) {
                formattedString += weeks + " week ";
            } else {
                formattedString += weeks + " weeks ";
            }
        }
        if (days != 0) {
            if (days == 1) {
                formattedString += days + " day ";
            } else {
                formattedString += days + " days ";
            }
        }
        return formattedString;

    }
}
