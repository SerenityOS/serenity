/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Represents strings that are arguments to relational constraints.
 * A <CODE>StringValueExp</CODE> may be used anywhere a <CODE>ValueExp</CODE> is required.
 *
 * @since 1.5
 */
public class StringValueExp implements ValueExp   {

    /* Serial version */
    private static final long serialVersionUID = -3256390509806284044L;

    /**
     * @serial The string literal
     */
    private String val;

    /**
     * Basic constructor.
     */
    public StringValueExp() {
    }

    /**
     * Creates a new <CODE>StringValueExp</CODE> representing the
     * given string.
     *
     * @param val the string that will be the value of this expression
     */
    public StringValueExp(String val) {
        this.val = val;
    }

    /**
     * Returns the string represented by the
     * <CODE>StringValueExp</CODE> instance.
     *
     * @return the string.
     */
    public String getValue()  {
        return val;
    }

    /**
     * Returns the string representing the object.
     */
    public String toString()  {
        return "'" + val.replace("'", "''") + "'";
    }


    /**
     * Sets the MBean server on which the query is to be performed.
     *
     * @param s The MBean server on which the query is to be performed.
     */
    /* There is no need for this method, because if a query is being
       evaluated a StringValueExp can only appear inside a QueryExp,
       and that QueryExp will itself have done setMBeanServer.  */
    @Deprecated
    public void setMBeanServer(MBeanServer s)  { }

    /**
     * Applies the ValueExp on a MBean.
     *
     * @param name The name of the MBean on which the ValueExp will be applied.
     *
     * @return  The <CODE>ValueExp</CODE>.
     *
     * @throws BadStringOperationException {@inheritDoc}
     * @throws BadBinaryOpValueExpException {@inheritDoc}
     * @throws BadAttributeValueExpException {@inheritDoc}
     * @throws InvalidApplicationException  {@inheritDoc}
     */
    @Override
    public ValueExp apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException,
        BadAttributeValueExpException, InvalidApplicationException  {
        return this;
    }
 }
