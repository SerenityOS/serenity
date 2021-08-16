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
  * This exception is thrown when a method
  * in some ways violates the schema. An example of schema violation
  * is modifying attributes of an object that violates the object's
  * schema definition. Another example is renaming or moving an object
  * to a part of the namespace that violates the namespace's
  * schema definition.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see javax.naming.Context#bind
  * @see DirContext#bind
  * @see javax.naming.Context#rebind
  * @see DirContext#rebind
  * @see DirContext#createSubcontext
  * @see javax.naming.Context#createSubcontext
  * @see DirContext#modifyAttributes
  * @since 1.3
  */
public class SchemaViolationException extends NamingException {
    /**
     * Constructs a new instance of SchemaViolationException.
     * All fields are set to null.
     */
    public SchemaViolationException() {
        super();
    }

    /**
     * Constructs a new instance of SchemaViolationException
     * using the explanation supplied. All other fields are set to null.
     * @param explanation Detail about this exception. Can be null.
     * @see java.lang.Throwable#getMessage
     */
    public SchemaViolationException(String explanation) {
        super(explanation);
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -3041762429525049663L;
}
