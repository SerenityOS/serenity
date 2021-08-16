/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.tool;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;;

final class ExecuteHelper {

    public static Object[] array;

    static class CustomEvent extends Event {
        int intValue;
        long longValue;
        double doubliValue;
        float floatValue;
        String stringValue;
        Short shortValue;
        boolean booleanValue;
        char charValue;
        double trickyDouble;
    }

    public static void emitCustomEvents() {
        // Custom events with potentially tricky values
        CustomEvent event1 = new CustomEvent();
        event1.trickyDouble = Double.NaN;
        event1.intValue = Integer.MIN_VALUE;
        event1.longValue = Long.MIN_VALUE;
        event1.doubliValue = Double.MIN_VALUE;
        event1.floatValue = Float.MIN_VALUE;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 512; i++) {
            sb.append((char) i);
        }
        sb.append("\u2324");
        event1.stringValue = sb.toString();
        event1.shortValue = Short.MIN_VALUE;
        event1.booleanValue = true;
        event1.booleanValue = false;
        event1.charValue = '\b';
        event1.commit();

        CustomEvent event2 = new CustomEvent();
        event2.trickyDouble = Double.NEGATIVE_INFINITY;
        event2.intValue = Integer.MAX_VALUE;
        event2.longValue = Long.MAX_VALUE;
        event2.doubliValue = Double.MAX_VALUE;
        event2.floatValue = Float.MAX_VALUE;
        event2.stringValue = null;
        event2.shortValue = Short.MAX_VALUE;
        event2.booleanValue = false;
        event2.charValue = 0;
        event2.commit();
    }

    public static Path createProfilingRecording() throws Exception {
        Path file = Utils.createTempFile("profiling-recording", ".jfr");
        // Create a recording with some data
        try (Recording r = new Recording(Configuration.getConfiguration("profile"))) {
            r.start();

            // Allocation event
            array = new Object[1000000];
            array = null;

            // Class loading event etc
            provokeClassLoading();

            // GC events
            System.gc();

            // ExecutionSample
            long t = System.currentTimeMillis();
            while (System.currentTimeMillis() - t < 50) {
                // do nothing
            }

            // Other periodic events, i.e CPU load
            Thread.sleep(1000);

            r.stop();
            r.dump(file);
        }

        return file;
    }

    private static void provokeClassLoading() {
       // Matching a string with regexp
       // is expected to load some classes and generate some VM events
       Pattern p = Pattern.compile("a*b");
       Matcher m = p.matcher("aaaaab");
       m.matches();
    }

    public static OutputAnalyzer jfr(String... args) throws Throwable {
        JDKToolLauncher l = JDKToolLauncher.createUsingTestJDK("jfr");
        for (String arg : args) {
            l.addToolArg(arg);
        }
        return ProcessTools.executeCommand(l.getCommand());
    }
}
