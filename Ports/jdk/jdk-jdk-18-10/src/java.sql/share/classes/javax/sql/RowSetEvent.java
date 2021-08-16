/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql;

/**
 * An {@code Event} object generated when an event occurs to a
 * {@code RowSet} object.  A {@code RowSetEvent} object is
 * generated when a single row in a rowset is changed, the whole rowset
 * is changed, or the rowset cursor moves.
 * <P>
 * When an event occurs on a {@code RowSet} object, one of the
 * {@code RowSetListener} methods will be sent to all registered
 * listeners to notify them of the event.  An {@code Event} object
 * is supplied to the {@code RowSetListener} method so that the
 * listener can use it to find out which {@code RowSet} object is
 * the source of the event.
 *
 * @since 1.4
 */

public class RowSetEvent extends java.util.EventObject {

  /**
   * Constructs a {@code RowSetEvent} object initialized with the
   * given {@code RowSet} object.
   *
   * @param source the {@code RowSet} object whose data has changed or
   *        whose cursor has moved
   * @throws IllegalArgumentException if {@code source} is null.
   */
  public RowSetEvent(RowSet source)
    { super(source); }

  /**
   * Private serial version unique ID to ensure serialization
   * compatibility.
   */
  static final long serialVersionUID = -1875450876546332005L;
}
