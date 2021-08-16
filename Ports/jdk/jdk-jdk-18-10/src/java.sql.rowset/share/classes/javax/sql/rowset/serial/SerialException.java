/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset.serial;

import java.sql.SQLException;

/**
 * Indicates and an error with the serialization or de-serialization of
 * SQL types such as <code>BLOB, CLOB, STRUCT or ARRAY</code> in
 * addition to SQL types such as <code>DATALINK and JAVAOBJECT</code>
 *
 * @since 1.5
 */
public class SerialException extends java.sql.SQLException {

    /**
     * Creates a new <code>SerialException</code> without a
     * message.
     */
     public SerialException() {
     }

    /**
     * Creates a new <code>SerialException</code> with the
     * specified message.
     *
     * @param msg the detail message
     */
    public SerialException(String msg) {
        super(msg);
    }

    static final long serialVersionUID = -489794565168592690L;
}
