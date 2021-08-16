/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.java.time;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.testng.annotations.Test;

/**
 * Test instant source.
 */
@Test
public class TestInstantSource {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    public void test_system() {
        // main tests for Clock.currentInstant() are in TestClock_System
        var test = InstantSource.system();
        assertSame(test.withZone(ZoneOffset.UTC), Clock.systemUTC());
        assertEquals(test.withZone(PARIS), Clock.system(PARIS));
        var millis = System.currentTimeMillis();
        var testMillis = test.millis();
        var testInstantMillis = test.instant().toEpochMilli();
        assertTrue(Math.abs(testMillis - millis) < 1000);
        assertTrue(Math.abs(testInstantMillis - millis) < 1000);
        assertSame(test, InstantSource.system());
        assertEquals(test.hashCode(), InstantSource.system().hashCode());
        assertEquals(test.toString(), "SystemInstantSource");
    }

    public void test_tick() {
        var millis = 257265861691L;
        var instant = Instant.ofEpochMilli(millis);
        var duration = Duration.ofSeconds(1);
        var test = InstantSource.tick(InstantSource.fixed(instant), duration);
        assertEquals(test.withZone(ZoneOffset.UTC), Clock.tick(Clock.fixed(instant, ZoneOffset.UTC), duration));
        assertEquals(test.withZone(PARIS), Clock.tick(Clock.fixed(instant, PARIS), duration));
        assertEquals(test.millis(), (millis / 1000) * 1000);
        assertEquals(test.instant(), instant.truncatedTo(SECONDS));
        assertEquals(test, InstantSource.tick(InstantSource.fixed(instant), duration));
        assertEquals(test.hashCode(), InstantSource.tick(InstantSource.fixed(instant), duration).hashCode());
    }

    public void test_fixed() {
        var millis = 257265861691L;
        var instant = Instant.ofEpochMilli(millis);
        var test = InstantSource.fixed(instant);
        assertEquals(test.withZone(ZoneOffset.UTC), Clock.fixed(instant, ZoneOffset.UTC));
        assertEquals(test.withZone(PARIS), Clock.fixed(instant, PARIS));
        assertEquals(test.millis(), millis);
        assertEquals(test.instant(), instant);
        assertEquals(test, InstantSource.fixed(instant));
        assertEquals(test.hashCode(), InstantSource.fixed(instant).hashCode());
    }

    public void test_offset() {
        var millis = 257265861691L;
        var instant = Instant.ofEpochMilli(millis);
        var duration = Duration.ofSeconds(120);
        var test = InstantSource.offset(InstantSource.fixed(instant), duration);
        assertEquals(test.withZone(ZoneOffset.UTC), Clock.offset(Clock.fixed(instant, ZoneOffset.UTC), duration));
        assertEquals(test.withZone(PARIS), Clock.offset(Clock.fixed(instant, PARIS), duration));
        assertEquals(test.millis(), millis + 120_000);
        assertEquals(test.instant(), instant.plusSeconds(120));
        assertEquals(test, InstantSource.offset(InstantSource.fixed(instant), duration));
        assertEquals(test.hashCode(), InstantSource.offset(InstantSource.fixed(instant), duration).hashCode());
    }

    static class MockInstantSource implements InstantSource {
        static final Instant FIXED = Instant.now();

        @Override
        public Instant instant() {
            return FIXED;
        }
    }

    public void test_mock() {
        var test = new MockInstantSource();
        assertEquals(test.withZone(ZoneOffset.UTC).getZone(), ZoneOffset.UTC);
        assertEquals(test.withZone(PARIS).getZone(), PARIS);
        assertEquals(test.withZone(ZoneOffset.UTC).withZone(PARIS).getZone(), PARIS);
        assertEquals(test.millis(), MockInstantSource.FIXED.toEpochMilli());
        assertEquals(test.instant(), MockInstantSource.FIXED);
        assertEquals(test.withZone(ZoneOffset.UTC), test.withZone(ZoneOffset.UTC));
        assertEquals(test.withZone(ZoneOffset.UTC).hashCode(), test.withZone(ZoneOffset.UTC).hashCode());
    }

}
