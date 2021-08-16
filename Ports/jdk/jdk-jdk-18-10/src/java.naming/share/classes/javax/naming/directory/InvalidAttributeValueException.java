/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.directory;

import javax.naming.NamingException;

/**
  * This class is thrown when an attempt is
  * made to add to an attribute a value that conflicts with the attribute's
  * schema definition.  This could happen, for example, if attempting
  * to add an attribute with no value when the attribute is required
  * to have at least one value, or if attempting to add more than
  * one value to a single valued-attribute, or if attempting to
  * add a value that conflicts with the syntax of the attribute.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

public class InvalidAttributeValueException extends NamingException {
    /**
     * Constructs a new instance of InvalidAttributeValueException using
     * an explanation. All other fields are set to null.
     * @param   explanation     Additional detail about this exception. Can be null.
     * @see java.lang.Throwable#getMessage
     */
    public InvalidAttributeValueException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs a new instance of InvalidAttributeValueException.
      * All fields are set to null.
      */
    public InvalidAttributeValueException() {
        super();
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 8720050295499275011L;
}
