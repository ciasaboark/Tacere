/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.converter;

import junit.framework.TestCase;

import org.junit.Test;

import static org.junit.Assert.*;

public class DateConverterTest extends TestCase {

    @Test
    public void testSevenDaysIsOneWeek() {
        DateConverter dc = new DateConverter(7);
        assertEquals(dc.getWeeks(), 1);
    }

    @Test
    public void testThirtyDaysIsOneMonth() {
        DateConverter dc = new DateConverter(30);
        assertEquals(dc.getMonths(), 1);
    }

    @Test
    public void test365DaysIsOneYear() {
        DateConverter dc = new DateConverter(365);
        assertEquals(dc.getYears(), 1);
    }

    @Test
    public void test27Days() {
        DateConverter dc = new DateConverter(27);
        assertTrue(dc.toString().equals("3 weeks 6 days"));
    }

    @Test
    public void testShouldFail() {
        assertEquals(true, false);
    }
}