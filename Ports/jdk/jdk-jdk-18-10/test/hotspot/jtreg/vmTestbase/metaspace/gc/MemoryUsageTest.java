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

/**
 * Test observers the progress on used/committed memory.
 * MemoryPoolMXBean is used for that purpose.
 *
 * Depending on command line option the test checks either Metaspace or
 * Compressed Class Space area.
 *
 * This test checks two things:
 * 1) Loading/Unloading classes doesn't cause memory increase
 * 2) Loading classes causes permanent increase of memory.
 */
public class MemoryUsageTest extends MetaspaceBaseGC {

    private String pool_name;

    public static void main(String[] args) {
        new MemoryUsageTest().run(args);
    }



    /**
     * Loads new classes by bunches and invokes GC after each bunch.
     * Expected behavior: used/committed should stop growing after 5 iterations.
     */
    public void checkForNotGrowing() {

        long p_used = 0;
        long p_committed = 0;

        System.out.println("%%%% Loading classes without storing refs, invoking gc manually");
        final int numberOfIteration = 10;
        for (int i = 0; i < numberOfIteration; i++) {
            loadNewClasses(500, false);
            gc();
            printMemoryUsage("% " + i + "  ");
            if (i == numberOfIteration / 2) {
                // used/committed in the middle of the step.
                p_used = getUsed();
                p_committed = getCommitted();
            }

        }
        long used = getUsed();
        long committed = getCommitted();

        // loading classes without keeping references to them
        // should not affect used/commited metaspace
        // but OK, let's allow some noise such as +/-8K
        if (Math.abs((int) (used - p_used)) > 1024*8) {
            throw new Fault("Used amount should be stable: " +
                    p_used + " --> " + used);
        }
        if (Math.abs((int) (committed - p_committed)) > 1024*8) {
            throw new Fault("Committed amount should be stable: " +
                    p_committed + " --> " + committed);
        }

    }

    /**
     * Loads new classes by bunches and invokes GC after each bunch.
     * Expected behavior: used/committed should keep growing
     */
    public void checkForGrowing() {
        long used = 0;
        long committed = 0;
        long p_used = 0 ;
        long p_committed = 0;

        // loading new classes, starting to keep references.
        // both used and commited metaspace should grow up.
        System.out.println("%%%% Loading classes, refs are stored, gc is invoking manually");
        for (int i = 0; i < 10; i++) {
            try {
                loadNewClasses(1000, true);
            } catch (OutOfMemoryError oom) {
                String message = oom.getMessage().toLowerCase();
                if (message.contains("metaspace") || message.contains("compressed class space")) {
                     System.out.println("% oom is ok: " + oom);
                     return;
                } else {
                     System.err.println("% unexpected OOM" + oom);
                     throw new Fault(oom);
                }
            }

            gc();
            printMemoryUsage("% " + i + "  ");
            p_used      = used;
            p_committed = committed;
            used = getUsed();
            committed = getCommitted();
            if (i > 0 && used <= p_used) {
                throw new Fault("Used amount reduced unexpectedly " +
                        p_used + " --> " + used);
            }
            if (i > 0 && committed < p_committed) {
                throw new Fault("Used amount reduced unexpectedly " +
                        p_committed + " --> " + committed);
            }
        }
    }

    /**
     * Looks up for memory pool name.
     * @param args command line options
     */
    @Override
    protected void parseArgs(String[] args) {
        if (args.length != 1) {
            printUsage();
            throw new Fault("MemoryPool is not specified");
        }

        String a = args[0];
        if (a.equalsIgnoreCase("-pool:compressed")) {
             pool_name = "Compressed Class Space";
        } else if (a.equalsIgnoreCase("-pool:metaspace")) {
             pool_name = "Metaspace";
        } else {
            printUsage();
            throw new Fault("Unrecongnized argument: " + a);
        }
    }

    private void printUsage() {
        System.err.println("Usage: ");
        System.err.println("java [-Xms..] [-XX:MetaspaceSize=..]  [-XX:MaxMetaspaceSize=..] \\");
        System.err.println("    " + MemoryUsageTest.class.getCanonicalName() + " -pool:<metaspace|compressed>");
    }

    /**
     * @return name of the MemoryPoolMXBean under test
     */
    @Override
    protected String getPoolName() {
        return pool_name;
    }

    @Override
    protected void doCheck() {
        checkForNotGrowing();
        checkForGrowing();
    }

}
