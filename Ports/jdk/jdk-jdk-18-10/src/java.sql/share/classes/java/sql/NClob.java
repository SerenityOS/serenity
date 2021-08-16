/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

/**
 * The mapping in the Java programming language
 * for the SQL {@code NCLOB} type.
 * An SQL {@code NCLOB} is a built-in type
 * that stores a Character Large Object using the National Character Set
 *  as a column value in a row of  a database table.
 * <P>The {@code NClob} interface extends the {@code Clob} interface
 * which provides methods for getting the
 * length of an SQL {@code NCLOB} value,
 * for materializing a {@code NCLOB} value on the client, and for
 * searching for a substring or {@code NCLOB} object within a
 * {@code NCLOB} value. A {@code NClob} object, just like a {@code Clob} object, is valid for the duration
 * of the transaction in which it was created.
 * Methods in the interfaces {@link ResultSet},
 * {@link CallableStatement}, and {@link PreparedStatement}, such as
 * {@code getNClob} and {@code setNClob} allow a programmer to
 * access an SQL {@code NCLOB} value.  In addition, this interface
 * has methods for updating a {@code NCLOB} value.
 * <p>
 * All methods on the {@code NClob} interface must be fully implemented if the
 * JDBC driver supports the data type.
 *
 * @since 1.6
 */

public interface NClob extends Clob { }
