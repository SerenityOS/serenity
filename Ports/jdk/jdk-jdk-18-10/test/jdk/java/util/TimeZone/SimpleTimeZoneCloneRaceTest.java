/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Calendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.function.Supplier;

/*
 * @test
 * @bug 8191216
 * @summary test that provokes race between cloning and lazily initializing
 *          SimpleTimeZone cache fields
 */
public class SimpleTimeZoneCloneRaceTest {

    public static void main(String[] args) throws InterruptedException {

        // shared TZ user repeatedly samples sharedTZ and calculates offset
        // using the shared instance
        TimeZoneUser sharedTZuser = new TimeZoneUser(() -> sharedTZ);

        // cloned TZ user repeatedly samples sharedTZ then clones it and
        // calculates offset using the clone...
        TimeZoneUser clonedTZuser = new TimeZoneUser(() -> {
            // sample shared TZ
            TimeZone tz = sharedTZ;
            // do some computation that takes roughly the same time as it takes
            // sharedTZUser to start changing cache fields in shared TZ
            cpuHogTZ.getOffset(time);
            // now clone the sampled TZ and return it, hoping the clone is done
            // at about the right time....
            return (TimeZone) tz.clone();
        });

        // start threads
        Thread t1 = new Thread(sharedTZuser);
        Thread t2 = new Thread(clonedTZuser);
        t1.start();
        t2.start();

        // plant new SimpleTimeZone instances for 2 seconds
        long t0 = System.currentTimeMillis();
        do {
            TimeZone tz1 = createSTZ();
            TimeZone tz2 = createSTZ();
            cpuHogTZ = tz1;
            sharedTZ = tz2;
        } while (System.currentTimeMillis() - t0 < 2000L);

        sharedTZuser.stop = true;
        clonedTZuser.stop = true;
        t1.join();
        t2.join();

        System.out.println(
            String.format("shared TZ: %d correct, %d incorrect calculations",
                          sharedTZuser.correctCount, sharedTZuser.incorrectCount)
        );
        System.out.println(
            String.format("cloned TZ: %d correct, %d incorrect calculations",
                          clonedTZuser.correctCount, clonedTZuser.incorrectCount)
        );
        if (clonedTZuser.incorrectCount > 0) {
            throw new RuntimeException(clonedTZuser.incorrectCount +
                                       " fatal data races detected");
        }
    }

    static SimpleTimeZone createSTZ() {
        return new SimpleTimeZone(-28800000,
                                  "America/Los_Angeles",
                                  Calendar.APRIL, 1, -Calendar.SUNDAY,
                                  7200000,
                                  Calendar.OCTOBER, -1, Calendar.SUNDAY,
                                  7200000,
                                  3600000);
    }

    static volatile TimeZone cpuHogTZ = createSTZ();
    static volatile TimeZone sharedTZ = createSTZ();
    static final long time;
    static final long correctOffset;

    static {
        TimeZone tz = createSTZ();
        Calendar cal = Calendar.getInstance(tz, Locale.ROOT);
        cal.set(2000, Calendar.MAY, 1, 0, 0, 0);
        time = cal.getTimeInMillis();
        correctOffset = tz.getOffset(time);
    }

    static class TimeZoneUser implements Runnable {
        private final Supplier<? extends TimeZone> tzSupplier;

        TimeZoneUser(Supplier<? extends TimeZone> tzSupplier) {
            this.tzSupplier = tzSupplier;
        }

        volatile boolean stop;
        int correctCount, incorrectCount;

        @Override
        public void run() {
            while (!stop) {
                TimeZone tz = tzSupplier.get();
                int offset = tz.getOffset(time);
                if (offset == correctOffset) {
                    correctCount++;
                } else {
                    incorrectCount++;
                }
            }
        }
    }
}
