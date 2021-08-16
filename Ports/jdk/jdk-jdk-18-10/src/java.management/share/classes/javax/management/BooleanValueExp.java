/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This class represents a boolean value. A BooleanValueExp may be
 * used anywhere a ValueExp is required.
 * @serial include
 *
 * @since 1.5
 */
class BooleanValueExp extends QueryEval implements ValueExp {

    /* Serial version */
    private static final long serialVersionUID = 7754922052666594581L;

    /**
     * @serial The boolean value
     */
    private boolean val = false;


    /** Creates a new BooleanValueExp representing the boolean literal {@code val}.*/
    BooleanValueExp(boolean val) {
        this.val = val;
    }

    /**Creates a new BooleanValueExp representing the Boolean object {@code val}.*/
    BooleanValueExp(Boolean val) {
        this.val = val.booleanValue();
    }


    /** Returns the  Boolean object representing the value of the BooleanValueExp object.*/
    public Boolean getValue()  {
        return Boolean.valueOf(val);
    }

    /**
     * Returns the string representing the object.
     */
    public String toString()  {
        return String.valueOf(val);
    }

    /**
     * Applies the ValueExp on a MBean.
     *
     * @param name The name of the MBean on which the ValueExp will be applied.
     *
     * @return  The <CODE>ValueExp</CODE>.
     *
     * @exception BadStringOperationException
     * @exception BadBinaryOpValueExpException
     * @exception BadAttributeValueExpException
     * @exception InvalidApplicationException
     */
    public ValueExp apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException,
        BadAttributeValueExpException, InvalidApplicationException  {
        return this;
    }

    @Deprecated
    public void setMBeanServer(MBeanServer s) {
        super.setMBeanServer(s);
    }


 }
