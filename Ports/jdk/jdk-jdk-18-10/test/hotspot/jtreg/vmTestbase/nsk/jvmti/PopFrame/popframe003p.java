/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.PopFrame;

import nsk.share.Wicket;
import java.io.PrintStream;

/**
 * This is auxiliary tested class
 */
public class popframe003p extends Thread {
    static final int PASSED = 0;
    static final int FAILED = 2;

    private PrintStream out;

    public static volatile int totRes = PASSED;
    public static Object barrier = new Object();
    public Wicket startingBarrier;

    // dummy global static fields
    static public byte bytePubStatGlFld = 1;
    static public short shortPubStatGlFld = 2;
    static public int intPubStatGlFld = 3;
    static public long longPubStatGlFld = 4L;
    static public float floatPubStatGlFld = 5.1F;
    static public double doublePubStatGlFld = 6.2D;
    static public char charPubStatGlFld = 'a';
    static public boolean booleanPubStatGlFld = false;
    static public String strPubStatGlFld = "static global field";

    // dummy instance fields
    byte byteGlFld = 11;
    short shortGlFld = 22;
    int intGlFld = 33;
    long longGlFld = 44L;
    private float floatGlFld = 55F;
    double doubleGlFld = 66D;
    char charGlFld = 'c';
    boolean booleanGlFld = true;
    String strGlFld = "instance field";
    Object objGl = this;

    // dummy public instance fields
    public byte bytePubGlFld = 0;
    public short shortPubGlFld = -2;
    public int intPubGlFld = -3;
    public long longPubGlFld = -4L;
    public float floatPubGlFld = -5.1F;
    public double doublePubGlFld = -6.2D;
    public char charPubGlFld = 'e';
    public boolean booleanPubGlFld = false;
    public String strPubGlFld = "public instance field";

    private popFrameCls popframeCls;

    popframe003p(String name, PrintStream out) {
        super(name);
        this.out = out;
        startingBarrier = new Wicket();
        popframeCls = new popFrameCls();
    }

    public void run() {
        popframeCls.activeMeth(1, Long.MAX_VALUE,
                               Double.MIN_VALUE, 'c', false);
        /* check that any changes for the global instance fields,
         * which occurred in the called method, remain
         */
        if (byteGlFld != 1 || shortGlFld != 2 ||
            intGlFld != 3 || longGlFld != 4L ||
            floatGlFld != 5.1F || doubleGlFld != 6.2D ||
            charGlFld != 'd' || booleanGlFld != false ||
            !strGlFld.equals("nstnc fld") || !objGl.equals(Integer.valueOf("1973")) ||
            bytePubGlFld != 7 || shortPubGlFld != 8 ||
            intPubGlFld != 9 || longPubGlFld != -10L ||
            floatPubGlFld != -11F || doublePubGlFld != -12D ||
            charPubGlFld != 'z' || booleanPubGlFld != false ||
            strPubGlFld != null) {
            out.println("TEST FAILED: changes for the instance fields of a class,\n" +
                "\twhich have been made in the popped frame's method, did not remain:\n" +
                "\tinstance fields values:\n\t\tbyteGlFld=" +
                byteGlFld + "\texpected: 1\n" +
                "\t\tshortGlFld=" + shortGlFld + "\texpected: 2\n" +
                "\t\tintGlFld=" + intGlFld + "\texpected: 3\n" +
                "\t\tlongGlFld=" + longGlFld + "\texpected: 4\n" +
                "\t\tfloatGlFld=" + floatGlFld + "\texpected: 5.1\n" +
                "\t\tdoubleGlFld=" + doubleGlFld + "\texpected: 6.2\n" +
                "\t\tcharGlFld='" + charGlFld + "'\texpected: 'd'\n" +
                "\t\tbooleanGlFld=" + booleanGlFld + "\texpected: false\n" +
                "\t\tstrGlFld=\"" + strGlFld + "\"\texpected: \"nstnc fld\"\n" +
                "\t\tobjGl=\"" + objGl.toString() + "\"\texpected: \"123\"\n" +
                "\tpublic instance fields values:\n\t\tbytePubGlFld=" +
                bytePubGlFld + "\texpected: 2\n" +
                "\t\tshortPubGlFld=" + shortPubGlFld + "\texpected: 3\n" +
                "\t\tintPubGlFld=" + intPubGlFld + "\texpected: 4\n" +
                "\t\tlongPubGlFld=" + longPubGlFld + "\texpected: 5\n" +
                "\t\tfloatPubGlFld=" + floatPubGlFld + "\texpected: 6.2\n" +
                "\t\tdoublePubGlFld=" + doublePubGlFld + "\texpected: 7.35\n" +
                "\t\tcharPubGlFld='" + charPubGlFld + "'\texpected: 'b'\n" +
                "\t\tbooleanPubGlFld=" + booleanPubGlFld + "\texpected: true\n" +
                "\t\tstrPubGlFld=\"" + strPubGlFld + "\"\texpected: null\n");
            totRes = FAILED;
        } else {
            out.println("Check #5 PASSED: changes for the instance fields of a class,\n" +
                    "\twhich have been made in the popped frame's method, remained\n" +
                    "popframe003p (" + this + "): exiting...");
        }
    }

    class popFrameCls {
        // dummy popFrameCls fields
        protected byte bytePubFld = 10;
        public short shortPubFld = 20;
        public int intPubFld = 30;
        public long longPubFld = 40L;
        public float floatPubFld = 50.1F;
        public double doublePubFld = 60.2D;
        public char charPubFld = 'b';
        public boolean booleanPubFld = true;
        protected String strPubFld = "static field";

        // flag to notify when exit from 'while' loop in activeMeth method
        public volatile boolean popFrameHasBeenDone = false;

        void activeMeth(int i, long l, double d, char c, boolean b) {
            boolean compl = true;

            if (popFrameHasBeenDone) { // popping has been done
                out.println("popframe003p (" + this + "): enter activeMeth() after popping");
                /* check that any changes for the arguments,
                 * which occurred in the called method, remain
                 */
                if (i != 2 || l != Long.MIN_VALUE ||
                        d != Double.MAX_VALUE || c != 'c' || b != true) {
                    out.println("TEST FAILED: changes for the arguments of " +
                        "the popped frame's method, did not remain\n" +
                        "\tcurrent argument values: i=" + i + " l=" + l +
                        " d=" + d + " c='" + c + "'\n");
                    totRes = FAILED;
                } else {
                    out.println("Check #3 PASSED: changes for the arguments of " +
                            "the popped frame's method, remained\n");
                }
                /* check that any changes for the class fields,
                 * which occurred in the called method, remain
                 */
                if (bytePubFld != 0 || shortPubFld != 0 ||
                        intPubFld != 0 || longPubFld != 0L ||
                        floatPubFld != 0F || doublePubFld != 0D ||
                        charPubFld != ' ' || booleanPubFld != false ||
                        !strPubFld.equals("static fld")) {
                    out.println("TEST FAILED: changes for the fields of an inner class,\n" +
                        "\twhich have been made in the popped frame's method, did not remain:\n" +
                        "\tstatic fields values:\n\t\tbytePubFld=" + bytePubFld + "\texpected: 0\n" +
                        "\t\tshortPubFld=" + shortPubFld + "\texpected: 0\n" +
                        "\t\tintPubFld=" + intPubFld + "\texpected: 0\n" +
                        "\t\tlongPubFld=" + longPubFld + "\texpected: 0\n" +
                        "\t\tfloatPubFld=" + floatPubFld + "\texpected: 0\n" +
                        "\t\tdoublePubFld=" + doublePubFld + "\texpected: 0\n" +
                        "\t\tcharPubFld='" + charPubFld + "'\texpected: ' '\n" +
                        "\t\tbooleanPubFld=" + booleanPubFld + "\texpected: false\n" +
                        "\t\tstrPubFld=\"" + strPubFld + "\"\texpected: \"static fld\"\n");
                    totRes = FAILED;
                } else {
                    out.println("Check #4 PASSED: changes for the fields of an inner class,\n" +
                            "\twhich have been made in the popped frame's method, remained\n" +
                            "popframe003p (" + this + "): exiting...\n");
                }

                return;
            }

            // make some variable changes:
            // for the arguments
            i = 2;
            l = Long.MIN_VALUE;
            d = Double.MAX_VALUE;
            b = true;

            // for the global static fields
            bytePubStatGlFld = 2;
            shortPubStatGlFld = 3;
            intPubStatGlFld = 4;
            longPubStatGlFld = 5L;
            floatPubStatGlFld = 6.2F;
            doublePubStatGlFld = 7.35D;
            charPubStatGlFld = 'b';
            booleanPubStatGlFld = true;
            strPubStatGlFld = "sttc glbl fld";

            // for the global instance fields
            byteGlFld = 1;
            shortGlFld = 2;
            intGlFld = 3;
            longGlFld = 4L;
            floatGlFld = 5.1F;
            doubleGlFld = 6.2D;
            charGlFld = 'd';
            booleanGlFld = false;
            strGlFld = "nstnc fld";
            objGl = Integer.valueOf(1973);

            // for the global public instance fields
            bytePubGlFld = 7;
            shortPubGlFld = 8;
            intPubGlFld = 9;
            longPubGlFld = -10L;
            floatPubGlFld = -11F;
            doublePubGlFld = -12D;
            charPubGlFld = 'z';
            booleanPubGlFld = false;
            strPubGlFld = null;

            // for the popFrameCls public fields
            bytePubFld = 0;
            shortPubFld = 0;
            intPubFld = 0;
            longPubFld = 0L;
            floatPubFld = 0F;
            doublePubFld = 0D;
            charPubFld = ' ';
            booleanPubFld = false;
            strPubFld = "static fld";

            try {
                // notify the main thread about readiness
                synchronized (barrier) {
                    out.println("popFrameCls (" + this + "): notifying main thread");
                    startingBarrier.unlock();
                    out.println("popFrameCls (" + this + "): inside activeMethod()");
                }
                // loop until the main thread pops us
                int ii = 0;
                int n = 1000;
                while (!popFrameHasBeenDone) {
                    if (n <= 0) {
                        n = 1000;
                    }
                    if (ii > n) {
                        ii = 0;
                        n--;
                    }
                    ii++;
                }

                // popping has been done
                out.println("TEST FAILED: a tested frame has not been really popped");
                totRes = FAILED;
                compl = false;

            } catch (Exception e) {
                out.println("FAILURE: popframe003p (" + this + "): caught " + e);
                totRes = FAILED;
                compl = false;
            } finally {
                if (compl) {
                    out.println("TEST FAILED: finally block was executed after PopFrame()");
                    totRes = FAILED;
                }
            }
        }
    }

    public void popFrameHasBeenDone() {
        popframeCls.popFrameHasBeenDone = true;
    }
}
