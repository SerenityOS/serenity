/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoubleJVMOption extends JVMOption {

    /**
     * Additional double values to test
     */
    private static final double ADDITIONAL_TEST_DOUBLE_NEGATIVE = -1.5;
    private static final double ADDITIONAL_TEST_DOUBLE_ZERO = 0.0;
    private static final double ADDITIONAL_TEST_DOUBLE_POSITIVE = 1.75;

    /**
     * Mininum option value
     */
    private double min;
    /**
     * Maximum option value
     */
    private double max;

    /**
     * Initialize double option with passed name
     *
     * @param name name of the option
     */
    DoubleJVMOption(String name) {
        this.name = name;
        min = Double.MIN_VALUE;
        max = Double.MAX_VALUE;
    }

    /**
     * Initialize double option with passed name, min and max values
     *
     * @param name name of the option
     * @param min minimum value of the option
     * @param max maximum value of the option
     */
    public DoubleJVMOption(String name, double min, double max) {
        this(name);
        this.min = min;
        this.max = max;
    }

    /**
     * Set new minimum option value
     *
     * @param min new minimum value
     */
    @Override
    void setMin(String min) {
        this.min = Double.valueOf(min);
    }

    /**
     * Get string with minimum value of the option
     *
     * @return string with minimum value of the option
     */
    @Override
    String getMin() {
        return formatValue(min);
    }

    /**
     * Set new maximum option value
     *
     * @param max new maximum value
     */
    @Override
    void setMax(String max) {
        this.max = Double.valueOf(max);
    }

    /**
     * Get string with maximum value of the option
     *
     * @return string with maximum value of the option
     */
    @Override
    String getMax() {
        return formatValue(max);
    }

    private String formatValue(double value) {
        return String.format(Locale.US, "%f", value);
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
            validValues.add(formatValue(min));
        }
        if (testMaxRange) {
            validValues.add(formatValue(max));
        }

        if (testMinRange) {
            if ((Double.compare(min, ADDITIONAL_TEST_DOUBLE_NEGATIVE) < 0)
                    && (Double.compare(max, ADDITIONAL_TEST_DOUBLE_NEGATIVE) > 0)) {
                validValues.add(formatValue(ADDITIONAL_TEST_DOUBLE_NEGATIVE));
            }

            if ((Double.compare(min, ADDITIONAL_TEST_DOUBLE_ZERO) < 0)
                    && (Double.compare(max, ADDITIONAL_TEST_DOUBLE_ZERO) > 0)) {
                validValues.add(formatValue(ADDITIONAL_TEST_DOUBLE_ZERO));
            }

            if ((Double.compare(min, ADDITIONAL_TEST_DOUBLE_POSITIVE) < 0)
                    && (Double.compare(max, ADDITIONAL_TEST_DOUBLE_POSITIVE) > 0)) {
                validValues.add(formatValue(ADDITIONAL_TEST_DOUBLE_POSITIVE));
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

        if (withRange) {
            /* Return invalid values only for options which have defined range in VM */
            if (Double.compare(min, Double.MIN_VALUE) != 0) {
                if ((Double.compare(min, 0.0) > 0)
                        && (Double.isNaN(min * 0.999) == false)) {
                    invalidValues.add(formatValue(min * 0.999));
                } else if ((Double.compare(min, 0.0) < 0)
                        && (Double.isNaN(min * 1.001) == false)) {
                    invalidValues.add(formatValue(min * 1.001));
                }
            }

            if (Double.compare(max, Double.MAX_VALUE) != 0) {
                if ((Double.compare(max, 0.0) > 0)
                        && (Double.isNaN(max * 1.001) == false)) {
                    invalidValues.add(formatValue(max * 1.001));
                } else if ((Double.compare(max, 0.0) < 0)
                        && (Double.isNaN(max * 0.999) == false)) {
                    invalidValues.add(formatValue(max * 0.999));
                }
            }
        }

        return invalidValues;
    }

    /**
     * Return expected error message for option with value "value" when it used
     * on command line with passed value
     *
     * @param value option value
     * @return expected error message
     */
    @Override
    protected String getErrorMessageCommandLine(String value) {
        String errorMsg;

        if (withRange) {
            /* Option have defined range in VM */
            errorMsg = "is outside the allowed range";
        } else {
            errorMsg = "";
        }

        return errorMsg;
    }
}
