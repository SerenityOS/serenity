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
 * relations.
 * @serial include
 *
 * @since 1.5
 */
class BetweenQueryExp extends QueryEval implements QueryExp {

    /* Serial version */
    private static final long serialVersionUID = -2933597532866307444L;

    /**
     * @serial The checked value
     */
    private ValueExp exp1;

    /**
     * @serial The lower bound value
     */
    private ValueExp exp2;

    /**
     * @serial The upper bound value
     */
    private ValueExp exp3;


    /**
     * Basic Constructor.
     */
    public BetweenQueryExp() {
    }

    /**
     * Creates a new BetweenQueryExp with v1 checked value, v2 lower bound
     * and v3 upper bound values.
     */
    public BetweenQueryExp(ValueExp v1, ValueExp v2, ValueExp v3) {
        exp1  = v1;
        exp2  = v2;
        exp3  = v3;
    }


    /**
     * Returns the checked value of the query.
     */
    public ValueExp getCheckedValue()  {
        return exp1;
    }

    /**
     * Returns the lower bound value of the query.
     */
    public ValueExp getLowerBound()  {
        return exp2;
    }

    /**
     * Returns the upper bound value of the query.
     */
    public ValueExp getUpperBound()  {
        return exp3;
    }

    /**
     * Applies the BetweenQueryExp on an MBean.
     *
     * @param name The name of the MBean on which the BetweenQueryExp will be applied.
     *
     * @return  True if the query was successfully applied to the MBean, false otherwise.
     *
     * @exception BadStringOperationException
     * @exception BadBinaryOpValueExpException
     * @exception BadAttributeValueExpException
     * @exception InvalidApplicationException
     */
    public boolean apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException,
        BadAttributeValueExpException, InvalidApplicationException  {
        ValueExp val1 = exp1.apply(name);
        ValueExp val2 = exp2.apply(name);
        ValueExp val3 = exp3.apply(name);
        boolean numeric = val1 instanceof NumericValueExp;

        if (numeric) {
            if (((NumericValueExp)val1).isLong()) {
                long lval1 = ((NumericValueExp)val1).longValue();
                long lval2 = ((NumericValueExp)val2).longValue();
                long lval3 = ((NumericValueExp)val3).longValue();
                return lval2 <= lval1 && lval1 <= lval3;
            } else {
                double dval1 = ((NumericValueExp)val1).doubleValue();
                double dval2 = ((NumericValueExp)val2).doubleValue();
                double dval3 = ((NumericValueExp)val3).doubleValue();
                return dval2 <= dval1 && dval1 <= dval3;
            }

        } else {
            String sval1 = ((StringValueExp)val1).getValue();
            String sval2 = ((StringValueExp)val2).getValue();
            String sval3 = ((StringValueExp)val3).getValue();
            return sval2.compareTo(sval1) <= 0 && sval1.compareTo(sval3) <= 0;
        }
    }

    /**
     * Returns the string representing the object.
     */
    @Override
    public String toString()  {
        return "(" + exp1 + ") between (" + exp2 + ") and (" + exp3 + ")";
    }
}
