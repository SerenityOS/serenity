/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package javax.management;


/**
 * This class is used by the query-building mechanism to represent binary
 * operations.
 * @serial include
 *
 * @since 1.5
 */
class InQueryExp extends QueryEval implements QueryExp {

    /* Serial version */
    private static final long serialVersionUID = -5801329450358952434L;

    /**
     * @serial The {@link ValueExp} to be found
     */
    private ValueExp val;

    /**
     * @serial The array of {@link ValueExp} to be searched
     */
    private ValueExp[]  valueList;


    /**
     * Basic Constructor.
     */
    public InQueryExp() {
    }

    /**
     * Creates a new InQueryExp with the specified ValueExp to be found in
     * a specified array of ValueExp.
     */
    public InQueryExp(ValueExp v1, ValueExp items[]) {
        val       = v1;
        valueList = items;
    }


    /**
     * Returns the checked value of the query.
     */
    public ValueExp getCheckedValue()  {
        return val;
    }

    /**
     * Returns the array of values of the query.
     */
    public ValueExp[] getExplicitValues()  {
        return valueList;
    }

    /**
     * Applies the InQueryExp on a MBean.
     *
     * @param name The name of the MBean on which the InQueryExp will be applied.
     *
     * @return  True if the query was successfully applied to the MBean, false otherwise.
     *
     * @exception BadStringOperationException
     * @exception BadBinaryOpValueExpException
     * @exception BadAttributeValueExpException
     * @exception InvalidApplicationException
     */
    public boolean apply(ObjectName name)
    throws BadStringOperationException, BadBinaryOpValueExpException,
        BadAttributeValueExpException, InvalidApplicationException  {
        if (valueList != null) {
            ValueExp v      = val.apply(name);
            boolean numeric = v instanceof NumericValueExp;

            for (ValueExp element : valueList) {
                element = element.apply(name);
                if (numeric) {
                    if (((NumericValueExp) element).doubleValue() ==
                        ((NumericValueExp) v).doubleValue()) {
                        return true;
                    }
                } else {
                    if (((StringValueExp) element).getValue().equals(
                        ((StringValueExp) v).getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the string representing the object.
     */
    public String toString()  {
        return val + " in (" + generateValueList() + ")";
    }


    private String generateValueList() {
        if (valueList == null || valueList.length == 0) {
            return "";
        }

        final StringBuilder result =
                new StringBuilder(valueList[0].toString());

        for (int i = 1; i < valueList.length; i++) {
            result.append(", ");
            result.append(valueList[i]);
        }

        return result.toString();
    }

 }
