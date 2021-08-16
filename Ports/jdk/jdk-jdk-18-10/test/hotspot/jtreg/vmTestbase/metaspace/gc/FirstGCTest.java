/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package metaspace.gc;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static metaspace.gc.MetaspaceBaseGC.PAGE_SIZE;

/**
 * Test for metaspace GC
 *
 * <ul>
 * <li>MetaspaceSize
 * </ul>
 * Test checks that the first GC happens when metaspace committed is next to
 * MetaspaceSize value.
 *
 * Based on actual events (JDK 8 GC tuning document)
 *
 * Quating: Java SE 8 HotSpot[tm] Virtual Machine Garbage Collection Tuning
 * <pre>
 * Class metadata is deallocated when the corresponding Java class is unloaded.
 * Java classes are unloaded as a results of garbage collection and garbage
 * collections may be induced in order to unload classes and deallocate class
 * metadata. When the space used for class metadata reaches a certain level
 * (call it a high-water mark), a garbage collection is induced.
 *
 * The flag MetaspaceSize can be set higher to avoid early garbage collections
 * induced for class metadata. The amount of class metadata allocated for
 * an application is application dependent and general guidelines do not
 * exist for the selection of MetaspaceSize. The default size of MetaspaceSize
 * is platform dependent and ranges from 12 MB to about 20 MB.
 * </pre>
 */
public class FirstGCTest extends MetaspaceBaseGC {
     /**
      * Current amount of the used metaspace
      */
     protected long used = 0;

     /**
      * Current amount of the committed metaspace
      */
     protected long committed = 0;

     /**
      * Previous amount of the used metaspace
      */
     protected long p_used = 0 ;

     /**
      * Previous amount of the committed metaspace
      */
     protected long p_committed = 0;

    public static void main(String... args) {
        new FirstGCTest().run(args);
    }

    // value given in -XX:metaspaceSize=<value>
    private long metaspaceSize = -1;


    @Override
    protected void parseArgs(String[] args) {
        final String XXSize = "-XX:MetaspaceSize=";
        for (String va: vmArgs) {
            if (va.startsWith(XXSize)) {
                metaspaceSize = parseValue(va.substring(XXSize.length()));
            }
        }
    }

    @Override
    protected String getPoolName() {
        return "Metaspace";
    }

    /**
     * Check for the first GC moment.
     *
     * Eats memory until GC is invoked (amount of used metaspace became less);
     * Checks that committed memory is close to MemaspaceSize.
     * Eats memory until the second GC to check min/max ratio options have effect.
     */
    @Override
    public void doCheck() {
        int gcCount = super.getMetaspaceGCCount();
        if (gcCount == 0) {
            // gc hasn't happened yet. Start loading classes.
            boolean gcHappened = this.eatMemoryUntilGC(50000);
            if (!gcHappened) {
                throw new Fault("GC hasn't happened");
            }
            System.out.println("% GC: " + super.lastGCLogLine());
            System.out.println("%   used     : " + p_used + " --> " + used);
            System.out.println("%   committed: " + p_committed + " --> " + committed);
            checkCommitted(p_committed);
        } else {
            // everything has happened before
            checkCommitted(detectCommittedFromGCLog());
        }
    }

    /**
     * Check that committed amount is close to expected value (MetaspaceSize)
     *
     * @param committedAmount - value to check
     */
    void checkCommitted(long committedAmount) {
        if (metaspaceSize > 0) {
            // -XX:MetaspaceSize is given
            if (Math.abs((int) (metaspaceSize - committedAmount)) < PAGE_SIZE) {
                System.out.println("% GC happened at the right moment");
                return;
            }
            if (!isMetaspaceGC()) {
                System.out.println("% GC wasn't induced by metaspace, cannot check the moment :(");
                return;
            }
            System.err.println("%## GC happened at the wrong moment, "
                    + "the amount of committed space significantly differs "
                    + "from the expected amount");
            System.err.println("%## Real    : " + committedAmount);
            System.err.println("%## Exepcted: " + metaspaceSize);
            throw new Fault("GC happened at the wrong moment");
        } else {
            // -XX:MetaspaceSize is not given, check for default values
            if (11_500_000 < committedAmount && committedAmount < 22_500_000) {
                System.out.println("% GC happened when the committed amout was from 12 MB to about 20 MB.");
                return;
            }
            if (!isMetaspaceGC()) {
                System.out.println("% GC wasn't induced by metaspace, this is excuse");
                return;
            }
            System.err.println("%## GC happened at the wrong moment, "
                    + "the amount of committed space was expected from 12 MB to about 20 MB");
            System.err.println("%## Real    : " + committedAmount);
            throw new Fault("It was the wrong moment when GC happened");
        }
    }

    /**
     * Load new classes without keeping references to them trying to provoke GC.
     * Stops if GC is detected, or number of attempts exceeds the given limit.
     *
     * @param times limit of attempts to provoke GC
     * @return true if GC has happened, false if limit has exceeded.
     */
    protected boolean eatMemoryUntilGC(int times) {
        System.out.println("%%%% Loading classes");
        System.out.println("% iter#  :   used  : commited");
        System.out.println("..............................");
        for (int i = 1; i < times; i++) {
            loadNewClasses(1, false);
            if (i % 1000 == 0) {
                printMemoryUsage("%  " + i + "  ");
            }
            p_used      = used;
            p_committed = committed;
            used = getUsed();
            committed = getCommitted();

            if (used < p_used) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the first full GC has already happened we will try to detect
     * the committed amount from the gc.log file.
     *
     * @return committed amount detected
     * @throws Fault if failed to detect.
     */
    protected long detectCommittedFromGCLog() {
        // parse gc.log to extract the committed value from string like:
        //  Metaspace       used 10133K, capacity 10190K, committed 10240K, reserved 10240Kl
        System.out.println("%%%% Parsing gc log to detect the moment of the first GC");
        String format = ".*Metaspace.* used .*, capacity .*, committed (\\d+)([KMGkmg]), reserved .*";
        Pattern p = Pattern.compile(format);
        try {
            for (String line: readGCLog()) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    int amount = Integer.parseInt(m.group(1));
                    int multi = 1;
                    switch (m.group(2).toLowerCase()) {
                        case "k": multi = 1024; break;
                        case "m": multi = 1024*1024; break;
                        case "g": multi = 1024*1024*1024; break;
                    }
                    long value = amount * multi;
                    System.out.println("% Committed detected: " + value);
                    return value;
                }
            }
        } catch (IOException e) {
            throw new Fault("Cannot read from the GC log");
        }
        System.out.println("% String that matches pattern '" + format + "' not found in the GC log file.");
        throw new Fault("Unable to detect the moment of GC from log file");
    }

}
