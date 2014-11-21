///*
// * Copyright (c) 2014 Jonathan Nelson
// * Released under the BSD license.  For details see the COPYING file.
// */
//
//package org.ciasaboark.tacere.event;
//
//import android.test.AndroidTestCase;
//
//import junit.framework.TestCase;
//import org.junit.Test;
//
//import org.ciasaboark.tacere.event.ringer.RingerType;
//import org.testng.annotations.Test;
//
//import static org.testng.Assert.assertEquals;
//import static org.testng.Assert.assertFalse;
//import static org.testng.Assert.assertNotNull;
//import static org.testng.Assert.assertTrue;
//
//public class EventInstanceTest extends TestCase {
//    @Test
//    public void testConstructorWithNullTitle() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, "description", 1, true, true);
//        assertNotNull(e.getTitle());
//        assertTrue("".equals(e.getTitle()));
//    }
//
//    @Test
//    public void testConstructorWithNullDescription() {
//        EventInstance e = new EventInstance(-1, -1, -1, "title", 0, 0, null, 1, true, true);
//        assertNotNull(e.getDescription());
//        assertTrue("".equals(e.getDescription()));
//    }
//
//    @Test
//    public void testDefaultRingerType() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, null, 1, true, true);
//        assertEquals(e.getRingerType(), RingerType.UNDEFINED);
//    }
//
//    @Test
//    public void testDefaultLocation() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, null, 1, true, true);
//        assertTrue("".equals(e.getLocation()));
//    }
//
//    @Test
//    public void testLocalBeginDateNotNullNotEmpty() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, null, 1, true, true);
//        assertNotNull(e.getLocalBeginDate());
//        assertTrue(!"".equals(e.getLocalBeginDate()));
//    }
//
//    @Test
//    public void testLocalBeginTimeNotNullNotEmpty() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, null, 1, true, true);
//        assertNotNull(e.getLocalBeginTime());
//        assertTrue(!"".equals(e.getLocalBeginTime()));
//    }
//
//    @Test
//    public void testLocalEndDateNotNullNotEmpty() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, null, 1, true, true);
//        assertNotNull(e.getLocalEndDate());
//        assertTrue(!"".equals(e.getLocalBeginDate()));
//    }
//
//    @Test
//    public void testLocalEndTimeNotNullNotEmpty() {
//        EventInstance e = new EventInstance(-1, -1, -1, null, 0, 0, null, 1, true, true);
//        assertNotNull(e.getLocalEndTime());
//        assertTrue(!"".equals(e.getLocalEndTime()));
//    }
//
//    @Test
//    void testEventActiveAtTime() {
//        final long NOW = System.currentTimeMillis();
//        final long MILLISECONDS_IN_HOUR = 1000 * 60 * 60;
//        final long HOUR_FROM_NOW = NOW + MILLISECONDS_IN_HOUR;
//
//        EventInstance e = new EventInstance(-1, -1, -1, null, NOW, HOUR_FROM_NOW, null, 1, true, true);
//        assertTrue(e.isActiveBetween(0, NOW), "Event should be active if begin == endTime");
//        assertTrue(e.isActiveBetween(NOW, NOW), "Event should be active if begin == endTime");
//        assertTrue(e.isActiveBetween(HOUR_FROM_NOW, HOUR_FROM_NOW), "Event should be active if end >= startTime");
//        assertTrue(e.isActiveBetween(HOUR_FROM_NOW, Long.MAX_VALUE), "Event should be active if end >= startTime");
//        assertTrue(e.isActiveBetween(NOW, HOUR_FROM_NOW));
//
//        assertFalse(e.isActiveBetween(0, NOW - 1), "Event should not be active if begin < endTime");
//        assertFalse(e.isActiveBetween(NOW + 1, Long.MAX_VALUE), "Event should not be active if end < startTime");
//    }
//
//
//}