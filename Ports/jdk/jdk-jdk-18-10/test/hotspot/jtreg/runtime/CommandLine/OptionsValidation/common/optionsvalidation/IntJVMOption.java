/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package optionsvalidation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.Platform;

public class IntJVMOption extends JVMOption {

    private static final BigInteger MIN_LONG;
    private static final BigInteger MAX_LONG;
    private static final BigInteger MAX_UNSIGNED_LONG;
    private static final BigInteger MAX_UNSIGNED_LONG_64;
    private static final BigInteger MINUS_ONE = new BigInteger("-1");
    private static final BigInteger TWO = new BigInteger("2");
    private static final BigInteger MIN_4_BYTE_INT = new BigInteger("-2147483648");
    private static final BigInteger MAX_4_BYTE_INT = new BigInteger("2147483647");
    private static final BigInteger MAX_4_BYTE_INT_PLUS_ONE = new BigInteger("2147483648");
    private static final BigInteger MAX_4_BYTE_UNSIGNED_INT = new BigInteger("4294967295");
    private static final BigInteger MAX_4_BYTE_UNSIGNED_INT_PLUS_ONE = new BigInteger("4294967296");

    /**
     * Mininum option value
     */
    private BigInteger min;

    /**
     * Maximum option value
     */
    private BigInteger max;

    /**
     * Option type: intx, uintx, size_t or uint64_t
     */
    private String type;

    /**
     * Is this value signed or unsigned
     */
    private boolean unsigned;

    /**
     * Is this 32 bit type
     */
    private boolean is32Bit = false;

    /**
     * Is this value 64 bit unsigned
     */
    private boolean uint64 = false;

    static {
        if (Platform.is32bit()) {
            MIN_LONG = new BigInteger(String.valueOf(Integer.MIN_VALUE));
            MAX_LONG = new BigInteger(String.valueOf(Integer.MAX_VALUE));
            MAX_UNSIGNED_LONG = MAX_LONG.multiply(TWO).add(BigInteger.ONE);
        } else {
            MIN_LONG = new BigInteger(String.valueOf(Long.MIN_VALUE));
            MAX_LONG = new BigInteger(String.valueOf(Long.MAX_VALUE));
            MAX_UNSIGNED_LONG = MAX_LONG.multiply(TWO).add(BigInteger.ONE);
        }

        MAX_UNSIGNED_LONG_64 = (new BigInteger(String.valueOf(Long.MAX_VALUE)))
                .multiply(TWO).add(BigInteger.ONE);
    }

    private IntJVMOption() {
        type = "";
    }

    /**
     * Initialize new integer option with given type. Type can be: INTX -
     * integer signed option UINTX - unsigned integer option UINT64_T - unsigned
     * 64 bit integer option
     *
     * @param name name of the option
     * @param type type of the option
     */
    IntJVMOption(String name, String type) {
        this.name = name;
        this.type = type;

        switch (type) {
            case "uint64_t":
                unsigned = true;
                uint64 = true;
                max = MAX_UNSIGNED_LONG_64;
                break;
            case "uintx":
            case "size_t":
                unsigned = true;
                max = MAX_UNSIGNED_LONG;
                break;
            case "uint":
                unsigned = true;
                is32Bit = true;
                max = MAX_4_BYTE_UNSIGNED_INT;
                break;
            case "int":
                min = MIN_4_BYTE_INT;
                max = MAX_4_BYTE_INT;
                is32Bit = true;
                break;
            default:
                min = MIN_LONG;
                max = MAX_LONG;
                break;
        }

        if (unsigned) {
            min = BigInteger.ZERO;
        }
    }

    /**
     * Initialize integer option with passed name, min and max values. Min and
     * max are string because they can be very big, bigger than long.
     *
     * @param name name of the option
     * @param min minimum value of the option
     * @param max maximum value of the option
     */
    public IntJVMOption(String name, String min, String max) {
        this();
        this.name = name;
        this.min = new BigInteger(min);
        this.max = new BigInteger(max);
    }

    /**
     * Set new minimum option value
     *
     * @param min new minimum value
     */
    @Override
    void setMin(String min) {
        this.min = new BigInteger(min);
    }

    /**
     * Get string with minimum value of the option
     *
     * @return string with minimum value of the option
     */
    @Override
    String getMin() {
        return min.toString();
    }

    /**
     * Set new maximum option value
     *
     * @param max new maximum value
     */
    @Override
    void setMax(String max) {
        this.max = new BigInteger(max);
    }

    /**
     * Get string with maximum value of the option
     *
     * @return string with maximum value of the option
     */
    @Override
    String getMax() {
        return max.toString();
    }

    /**
     * Return list of strings with valid option values which used for testing
     * using jcmd, attach and etc.
     *
     * @return list of strings which contain valid values for option
     */
    @Override
    protected List<String> getValidValues() {
        List<String> validValues = new ArrayList<>();

        if (testMinRange) {
            validValues.add(min.toString());
        }
        if (testMaxRange) {
            validValues.add(max.toString());
        }

        if (testMinRange) {
            if ((min.compareTo(MINUS_ONE) == -1) && (max.compareTo(MINUS_ONE) == 1)) {
                /*
                 * Add -1 as valid value if min is less than -1 and max is greater than -1
                 */
                validValues.add("-1");
            }

            if ((min.compareTo(BigInteger.ZERO) == -1) && (max.compareTo(BigInteger.ZERO) == 1)) {
                /*
                 * Add 0 as valid value if min is less than 0 and max is greater than 0
                 */
                validValues.add("0");
            }
            if ((min.compareTo(BigInteger.ONE) == -1) && (max.compareTo(BigInteger.ONE) == 1)) {
                /*
                 * Add 1 as valid value if min is less than 1 and max is greater than 1
                 */
                validValues.add("1");
            }
        }

        if (testMaxRange) {
            if ((min.compareTo(MAX_4_BYTE_INT_PLUS_ONE) == -1) && (max.compareTo(MAX_4_BYTE_INT_PLUS_ONE) == 1)) {
                /*
                 * Check for overflow when flag is assigned to the
                 * 4 byte int variable
                 */
                validValues.add(MAX_4_BYTE_INT_PLUS_ONE.toString());
            }

            if ((min.compareTo(MAX_4_BYTE_UNSIGNED_INT_PLUS_ONE) == -1) && (max.compareTo(MAX_4_BYTE_UNSIGNED_INT_PLUS_ONE) == 1)) {
                /*
                 * Check for overflow when flag is assigned to the
                 * 4 byte unsigned int variable
                 */
                validValues.add(MAX_4_BYTE_UNSIGNED_INT_PLUS_ONE.toString());
            }
        }

        return validValues;
    }

    /**
     * Return list of strings with invalid option values which used for testing
     * using jcmd, attach and etc.
     *
     * @return list of strings which contain invalid values for option
     */
    @Override
    protected List<String> getInvalidValues() {
        List<String> invalidValues = new ArrayList<>();

        /* Return invalid values only for options which have defined range in VM */
        if (withRange) {
            if (unsigned) {
                /* Only add non-negative out-of-range values for unsigned options */
                if (min.compareTo(BigInteger.ZERO) == 1) {
                    invalidValues.add(min.subtract(BigInteger.ONE).toString());
                }

                if ((is32Bit && (max.compareTo(MAX_4_BYTE_UNSIGNED_INT) != 0))
                        || (!is32Bit && !uint64 && (max.compareTo(MAX_UNSIGNED_LONG) != 0))
                        || (uint64 && (max.compareTo(MAX_UNSIGNED_LONG_64) != 0))) {
                    invalidValues.add(max.add(BigInteger.ONE).toString());
                }
            } else {
                if ((is32Bit && min.compareTo(MIN_4_BYTE_INT) != 0)
                        || (!is32Bit && min.compareTo(MIN_LONG) != 0)) {
                    invalidValues.add(min.subtract(BigInteger.ONE).toString());
                }
                if ((is32Bit && (max.compareTo(MAX_4_BYTE_INT) != 0))
                        || (!is32Bit && (max.compareTo(MAX_LONG) != 0))) {
                    invalidValues.add(max.add(BigInteger.ONE).toString());
                }
            }
        }

        return invalidValues;
    }

    /**
     * Return expected error message for option with value "value" when it used
     * on command line with passed value
     *
     * @param value Option value
     * @return expected error message
     */
    @Override
    protected String getErrorMessageCommandLine(String value) {
        String errorMsg;

        if (withRange) {
            /* Option have defined range in VM */
            if (unsigned && ((new BigInteger(value)).compareTo(BigInteger.ZERO) < 0)) {
                /*
                 * Special case for unsigned options with lower range equal to 0. If
                 * passed value is negative then error will be caught earlier for
                 * such options. Thus use different error message.
                 */
                errorMsg = String.format("Improperly specified VM option '%s=%s'", name, value);
            } else {
                errorMsg = String.format("%s %s=%s is outside the allowed range [ %s ... %s ]",
                        type, name, value, min.toString(), max.toString());
            }
        } else {
            errorMsg = "";
        }

        return errorMsg;
    }
}
