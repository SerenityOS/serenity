/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown when an attempt is made to apply either of the following: A
 * subquery expression to an MBean or a qualified attribute expression
 * to an MBean of the wrong class.  This exception is used internally
 * by JMX during the evaluation of a query.  User code does not
 * usually see it.
 *
 * @since 1.5
 */
public class InvalidApplicationException extends Exception   {


    /* Serial version */
    private static final long serialVersionUID = -3048022274675537269L;

    /**
     * @serial The object representing the class of the MBean
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object val;


    /**
     * Constructs an <CODE>InvalidApplicationException</CODE> with the specified <CODE>Object</CODE>.
     *
     * @param val the detail message of this exception.
     */
    public InvalidApplicationException(Object val) {
        this.val = val;
    }
}
