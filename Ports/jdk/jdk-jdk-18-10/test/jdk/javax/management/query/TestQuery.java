/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Class TestQuery
 * MBean used for testing the types wired when using QueryExp.
 * It is heavily linked to QueryFactory.
 */
public class TestQuery extends QueryData implements TestQueryMBean {

    /**
     * Attribute : BooleanAtt
     */
    private boolean booleanAtt = booleanValue;

    /**
     * Attribute : DoubleAtt
     */
    private double doubleAtt = doubleValue;

    /**
     * Attribute : FloatAtt
     */
    private float floatAtt = floatValue;

    /**
     * Attribute : IntAtt
     */
    private int intAtt = intValue;

    /**
     * Attribute : IntegerAtt
     */
    private Integer integerAtt = integerValue;

    /**
     * Attribute : LongAtt
     */
    private long longAtt = longValue;

    /**
     * Attribute : StringAtt
     */
    private String stringAtt = stringValue;

    public TestQuery() {
    }

    /**
     * Get Att of type boolean
     */
    public boolean getBooleanAtt() {
        return booleanAtt;
    }

    /**
     * Set Att of type boolean
     */
    public void setBooleanAtt(boolean value) {
        booleanAtt = value;
    }

    /**
     * Get Att of type double
     */
    public double getDoubleAtt() {
        return doubleAtt;
    }

    /**
     * Set Att of type double
     */
    public void setDoubleAtt(double value) {
        doubleAtt = value;
    }

    /**
     * Get Att of type float
     */
    public float getFloatAtt() {
        return floatAtt;
    }

    /**
     * Set Att of type float
     */
    public void setFloatAtt(float value) {
        floatAtt = value;
    }

    /**
     * Get Att of type int
     */
    public int getIntAtt() {
        return intAtt;
    }

    /**
     * Set Att of type int
     */
    public void setIntAtt(int value) {
        intAtt = value;
    }

    /**
     * Get Att of type Integer
     */
    public Integer getIntegerAtt() {
        return integerAtt;
    }

    /**
     * Set Att of type Integer
     */
    public void setIntegerAtt(Integer value) {
        integerAtt = value;
    }

    /**
     * Get Att of type long
     */
    public long getLongAtt() {
        return longAtt;
    }

    /**
     * Set Att of type long
     */
    public void setLongAtt(long value) {
        longAtt = value;
    }

    /**
     * Get Att of type String
     */
    public String getStringAtt() {
        return stringAtt;
    }

    /**
     * Set Att of type String
     */
    public void setStringAtt(String value) {
        stringAtt = value;
    }

}
