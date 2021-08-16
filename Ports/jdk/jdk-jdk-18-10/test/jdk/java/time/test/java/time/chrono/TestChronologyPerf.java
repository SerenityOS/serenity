/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package test.java.time.chrono;

import java.time.Duration;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.testng.annotations.Test;

/**
 * Test the speed of initializing all calendars.
 */
public class TestChronologyPerf {

    @Test
    public void test_chronologyGetAvailablePerf() {
        long start = System.nanoTime();
        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        long end = System.nanoTime();
        Duration d = Duration.of(end - start, ChronoUnit.NANOS);
        System.out.printf(" Cold Duration of Chronology.getAvailableChronologies(): %s%n", d);

        start = System.nanoTime();
        chronos = Chronology.getAvailableChronologies();
        end = System.nanoTime();
        d = Duration.of(end - start, ChronoUnit.NANOS);
        System.out.printf(" Warm Duration of Chronology.getAvailableChronologies(): %s%n", d);

        start = System.nanoTime();
        HijrahChronology.INSTANCE.date(1434, 1, 1);
        end = System.nanoTime();
        d = Duration.of(end - start, ChronoUnit.NANOS);
        System.out.printf(" Warm Duration of HijrahDate.date(1434, 1, 1): %s%n", d);
    }

}
