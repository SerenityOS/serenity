/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4614842
 * @summary Make sure that a Date and a GregorianCalendar produce the same date/time. Both are new implementations in 1.5.
 * @run main DateGregorianCalendarTest 15
 */

import java.util.*;
import static java.util.GregorianCalendar.*;

// Usage: java DateGregorianCalendarTest [duration]

@SuppressWarnings("deprecation")
public class DateGregorianCalendarTest {
    static volatile boolean runrun = true;
    static int nThreads;

    public static void main(String[] args) {
        int duration = 600;
        if (args.length == 1) {
            duration = Math.max(10, Integer.parseInt(args[0]));
        }

        TimeZone savedTZ = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            Thread[] t = new Thread[10]; // for future bugs...
            int index = 0;
            t[index++] = new Thread(new Runnable() {
                    public void run() {
                        GregorianCalendar gc = new GregorianCalendar();

                        long delta = (long)(279 * 365.2422 * 24 * 60 * 60 * 1000);
                        long count = 0;
                        try {
                            for (long t = Long.MIN_VALUE; runrun && t < Long.MAX_VALUE-delta; t += delta) {
                                gc.setTimeInMillis(t);
                                Date date = new Date(t);
                                int y;
                                if (!((y = gc.get(YEAR)) == (date.getYear()+1900) &&
                                      gc.get(MONTH) == date.getMonth() &&
                                      gc.get(DAY_OF_MONTH) == date.getDate() &&
                                      gc.get(HOUR_OF_DAY) == date.getHours() &&
                                      gc.get(MINUTE) == date.getMinutes() &&
                                      gc.get(SECOND) == date.getSeconds())) {
                                    throw new RuntimeException("GregorinCalendar and Date returned different dates."
                                                               +" (millis=" + t + ")\n"
                                                               +"GC=" + gc + "\nDate=" + date);
                                }
                                ++count;
                                if (y >= 1) {
                                    delta = (long)(365.2422 * 24 * 60 * 60 * 1000);
                                }
                                if (y >= 1970) {
                                    delta = (24 * 60 * 60 * 1000);
                                }
                                if (y >= 2039) {
                                    delta = (long)(279 * 365.2422 * 24 * 60 * 60 * 1000);
                                }
                            }
                            if (runrun) {
                                System.out.println("Part I (count="+count+"): Passed");
                            } else {
                                System.out.println("Part I (count="+count+"): Incomplete");
                            }
                        } catch (RuntimeException e) {
                            System.out.println("Part I (count="+count+"): FAILED");
                            runrun = false;
                            throw e;
                        } finally {
                            decrementCounter();
                        }
                    }
                });

            t[index++] = new Thread(new Runnable() {
                    public void run() {
                        GregorianCalendar gc = new GregorianCalendar();

                        long count = 0;
                        int delta;
                        try {
                            for (long year = Integer.MIN_VALUE+1900;
                                 runrun && year <= Integer.MAX_VALUE; year += delta) {
                                checkTimes(gc, year, JANUARY, 1, 0, 0, 0);
                                ++count;
                                delta = getDelta((int)year);
                            }

                            for (long month = Integer.MIN_VALUE;
                                 runrun && month <= Integer.MAX_VALUE; month += delta) {
                                checkTimes(gc, 1900, month, 1, 0, 0, 0);
                                ++count;
                                delta = getDelta(gc.get(YEAR));
                            }

                            for (long dayOfMonth = Integer.MIN_VALUE;
                                 runrun && dayOfMonth <= Integer.MAX_VALUE; dayOfMonth += delta) {
                                checkTimes(gc, 1900, JANUARY, dayOfMonth, 0, 0, 0);
                                ++count;
                                delta = getDelta(gc.get(YEAR));
                            }
                            if (runrun) {
                                System.out.println("Part II (count="+count+"): Passed");
                            } else {
                                System.out.println("Part II (count="+count+"): Incomplete");
                            }
                        } catch (RuntimeException e) {
                            System.out.println("Part II (count="+count+"): FAILED");
                            runrun = false;
                            throw e;
                        } finally {
                            decrementCounter();
                        }
                    }
                });

            // t3 takes more than 10 minutes (on Ultra-60 450MHz) without
            // the 4936355 fix due to getting the small delta.
            t[index++] = new Thread(new Runnable() {
                    public void run() {
                        GregorianCalendar gc = new GregorianCalendar();

                        long count = 0;
                        int delta;
                        try {
                            for (long hourOfDay = Integer.MIN_VALUE;
                                 runrun && hourOfDay <= Integer.MAX_VALUE; hourOfDay += delta) {
                                checkTimes(gc, 1970, JANUARY, 1, hourOfDay, 0, 0);
                                ++count;
                                delta = getDelta(gc.get(YEAR));
                            }
                            for (long minutes = Integer.MIN_VALUE;
                                 runrun && minutes <= Integer.MAX_VALUE; minutes += delta) {
                                checkTimes(gc, 1970, JANUARY, 1, 0, minutes, 0);
                                ++count;
                                delta = getDelta(gc.get(YEAR)) * 60;
                            }
                            for (long seconds = Integer.MIN_VALUE;
                                 runrun && seconds <= Integer.MAX_VALUE; seconds += delta) {
                                checkTimes(gc, 1970, JANUARY, 1, 0, 0, seconds);
                                ++count;
                                delta = getDelta(gc.get(YEAR)) * 60 * 60;
                            }
                            if (runrun) {
                                System.out.println("Part III (count="+count+"): Passed");
                            } else {
                                System.out.println("Part III (count="+count+"): Incomplete");
                            }
                        } catch (RuntimeException e) {
                            System.out.println("Part III (count="+count+"): FAILED");
                            runrun = false;
                            throw e;
                        } finally {
                            decrementCounter();
                        }
                    }
                });

            for (int i = 0; i < index; i++) {
                incrementCounter();
                t[i].start();
            }

            try {
                for (int i = 0; getCounter() > 0 && i < duration; i++) {
                    Thread.sleep(1000);
                }
                runrun = false;
                for (int i = 0; i < index; i++) {
                    t[i].join();
                }
            } catch (InterruptedException e) {
            }
        } finally {
            TimeZone.setDefault(savedTZ);
        }
    }

    static void checkTimes(GregorianCalendar gc, long year, long month, long dayOfMonth,
                           long hourOfDay, long minutes, long seconds) {
        gc.clear();
        gc.set((int)year, (int)month, (int)dayOfMonth, (int)hourOfDay, (int)minutes, (int)seconds);
        long time = gc.getTimeInMillis();
        Date date = new Date((int)year - 1900, (int)month, (int)dayOfMonth,
                             (int)hourOfDay, (int)minutes, (int)seconds);
        long time2 = date.getTime();
        if (time != time2) {
            throw new RuntimeException("GregorinCalendar and Date returned different values.\n"
                                       +"year="+year+", month="+month+", dayOfMonth="+dayOfMonth
                                       +"\nhourOfDay="+hourOfDay+", minutes="+minutes+", seconds="+seconds
                                       +"\ntime=" + time + ", time2=" + time2
                                       +"\nGC=" + gc + "\nDate=" + date);
        }
    }

    static final int getDelta(int year) {
        return (year >= 1970 && year <= 2039) ? 1 : 1<<13;
    }

    synchronized static void incrementCounter() {
        nThreads++;
    }

    synchronized static void decrementCounter() {
        nThreads--;
    }

    synchronized static int getCounter() {
        return nThreads;
    }
}
