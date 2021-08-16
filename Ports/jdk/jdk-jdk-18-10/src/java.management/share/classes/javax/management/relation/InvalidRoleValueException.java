/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.relation;

/**
 * Role value is invalid.
 * This exception is raised when, in a role, the number of referenced MBeans
 * in given value is less than expected minimum degree, or the number of
 * referenced MBeans in provided value exceeds expected maximum degree, or
 * one referenced MBean in the value is not an Object of the MBean
 * class expected for that role, or an MBean provided for that role does not
 * exist.
 *
 * @since 1.5
 */
public class InvalidRoleValueException extends RelationException {

    /* Serial version */
    private static final long serialVersionUID = -2066091747301983721L;

    /**
     * Default constructor, no message put in exception.
     */
    public InvalidRoleValueException() {
        super();
    }

    /**
     * Constructor with given message put in exception.
     *
     * @param message the detail message.
     */
    public InvalidRoleValueException(String message) {
        super(message);
    }
}
