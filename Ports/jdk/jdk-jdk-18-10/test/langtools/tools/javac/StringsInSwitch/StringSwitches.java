/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6827009 7071246
 * @summary Positive tests for strings in switch.
 * @author  Joseph D. Darcy
 */

public class StringSwitches {

    public static void main(String... args) {
        int failures = 0;

        failures += testPileup();
        failures += testSwitchingTwoWays();
        failures += testNamedBreak();
        failures += testExtraParens();

        if (failures > 0) {
            throw new RuntimeException();
        }
    }

    /*
     * A zero length string and all strings consisting only of the
     * zero character \u0000 have a hash code of zero.  This method
     * maps such strings to the number of times \u0000 appears for 0
     * through 6 occurrences.
     */
    private static int zeroHashes(String s) {
        int result = Integer.MAX_VALUE;
        switch(s) {
        case "":
            return 0;

        case "\u0000":
            result = 1; break;

        case "\u0000\u0000":
            return 2;

        case "\u0000\u0000\u0000":
            result = 3; break;

        case "\u0000\u0000\u0000\u0000":
            return 4;

        case "\u0000\u0000\u0000\u0000\u0000":
            result = 5; break;

        case "\u0000\u0000\u0000\u0000\u0000\u0000":
            return 6;

        default:
            result = -1;
        }
        return result;
    }

    private static int testPileup() {
        int failures = 0;
        String zero = "";
        for(int i = 0; i <= 6; i++, zero += "\u0000") {
            int result = zeroHashes(zero);
            if (result != i) {
                failures++;
                System.err.printf("For string \"%s\" unexpectedly got %d instead of %d%n.",
                                   zero, result, i);
            }
        }

        if (zeroHashes("foo") != -1) {
            failures++;
            System.err.println("Failed to get -1 for input string.");
        }

        return failures;
    }

    /**
     * Verify that a switch on an enum and a switch with the same
     * structure on the string name of an enum compute equivalent
     * values.
     */
    private static int testSwitchingTwoWays() {
        int failures = 0;

        for(MetaSynVar msv : MetaSynVar.values()) {
            int enumResult = enumSwitch(msv);
            int stringResult = stringSwitch(msv.name());

            if (enumResult != stringResult) {
                failures++;
                System.err.printf("One value %s, computed 0x%x with the enum switch " +
                                  "and 0x%x with the string one.%n",
                                  msv, enumResult, stringResult);
            }
        }

        return failures;
    }

    private static enum MetaSynVar {
        FOO,
        BAR,
        BAZ,
        QUX,
        QUUX,
        QUUUX,
        MUMBLE,
        FOOBAR;
    }

    private static int enumSwitch(MetaSynVar msv) {
        int result = 0;
        switch(msv) {
        case FOO:
            result |= (1<<0);
            // fallthrough:

        case BAR:
        case BAZ:
            result |= (1<<1);
            break;

        default:
            switch(msv) {
            case QUX:
                result |= (1<<2);
                break;

            case QUUX:
                result |= (1<<3);

            default:
                result |= (1<<4);
            }
            result |= (1<<5);
            break;

        case MUMBLE:
            result |= (1<<6);
            return result;

        case FOOBAR:
            result |= (1<<7);
            break;
        }
        result |= (1<<8);
        return result;
    }

    private static int stringSwitch(String msvName) {
        int result = 0;
        switch(msvName) {
        case "FOO":
            result |= (1<<0);
            // fallthrough:

        case "BAR":
        case "BAZ":
            result |= (1<<1);
            break;

        default:
            switch(msvName) {
            case "QUX":
                result |= (1<<2);
                break;

            case "QUUX":
                result |= (1<<3);

            default:
                result |= (1<<4);
            }
            result |= (1<<5);
            break;

        case "MUMBLE":
            result |= (1<<6);
            return result;

        case "FOOBAR":
            result |= (1<<7);
            break;
        }
        result |= (1<<8);
        return result;
    }

    private static int testNamedBreak() {
        int failures = 0;
        String[] testStrings  = {"a",       "b",  "c",       "d",      "e"};
        int[]    testExpected = { 0b101011, 0b101, 0b100001, 0b101000, 0b10000};

        for(int i = 0; i < testStrings.length; i++) {
            int expected = testExpected[i];
            int result = namedBreak(testStrings[i]);

            if (result != expected) {
                failures++;

                System.err.printf("On input %s, got %d instead of %d.%n",
                                  testStrings[i], result, expected);
            }
        }

        return failures;
    }

    private static int namedBreak(String s) {
        int result = 0;
        outer: switch(s) {
        case "a":
        case "b":
        case "c":
            result |= (1<<0);
        inner: switch(s + s) {
            case "aa":
                result |= (1<<1);
                break inner;

            case "cc":
                break outer;

            default:
                result |= (1<<2);
                return result;
            }

        case "d":
            result |= (1<<3);
            break outer;

        default:
            return result |= (1<<4);
        }
        result |= (1<<5);
        return result;
    }

    private static int testExtraParens() {
        int failures = 1;
        String s = "first";

        switch(s) {
        case (("first")):
            failures = 0;
            break;
        case ("second"):
            throw new RuntimeException("Should not be reached.");
        }

        return failures;
    }
}
