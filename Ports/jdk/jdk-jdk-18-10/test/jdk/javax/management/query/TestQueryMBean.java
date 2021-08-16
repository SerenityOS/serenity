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
 * Interface TestQueryMBean
 * MBean used for testing the types wired when using QueryExp.
 * It is heavily linked to QueryFactory.
 */
public interface TestQueryMBean
{
    /**
     * Get Att of type boolean
     */
    public boolean getBooleanAtt();

    /**
     * Set Att of type boolean
     */
    public void setBooleanAtt(boolean value);

    /**
     * Get Att of type double
     */
    public double getDoubleAtt();

    /**
     * Set Att of type double
     */
    public void setDoubleAtt(double value);

    /**
     * Get Att of type float
     */
    public float getFloatAtt();

    /**
     * Set Att of type float
     */
    public void setFloatAtt(float value);

    /**
     * Get Att of type int
     */
    public int getIntAtt();

    /**
     * Set Att of type int
     */
    public void setIntAtt(int value);

    /**
     * Get Att of type Integer
     */
    public Integer getIntegerAtt();

    /**
     * Set Att of type Integer
     */
    public void setIntegerAtt(Integer value);

    /**
     * Get Att of type long
     */
    public long getLongAtt();

    /**
     * Set Att of type long
     */
    public void setLongAtt(long value);

    /**
     * Get Att of type String
     */
    public String getStringAtt();

    /**
     * Set Att of type String
     */
    public void setStringAtt(String value);

}
