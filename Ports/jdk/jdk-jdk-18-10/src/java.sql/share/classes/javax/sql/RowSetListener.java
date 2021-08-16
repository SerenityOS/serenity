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
 * An interface that must be implemented by a
 * component that wants to be notified when a significant
 * event happens in the life of a {@code RowSet} object.
 * A component becomes a listener by being registered with a
 * {@code RowSet} object via the method {@code RowSet.addRowSetListener}.
 * How a registered component implements this interface determines what it does
 * when it is notified of an event.
 *
 * @since 1.4
 */

public interface RowSetListener extends java.util.EventListener {

  /**
   * Notifies registered listeners that a {@code RowSet} object
   * in the given {@code RowSetEvent} object has changed its entire contents.
   * <P>
   * The source of the event can be retrieved with the method
   * {@code event.getSource}.
   *
   * @param event a {@code RowSetEvent} object that contains
   *         the {@code RowSet} object that is the source of the event
   */
  void rowSetChanged(RowSetEvent event);

  /**
   * Notifies registered listeners that a {@code RowSet} object
   * has had a change in one of its rows.
   * <P>
   * The source of the event can be retrieved with the method
   * {@code event.getSource}.
   *
   * @param event a {@code RowSetEvent} object that contains
   *         the {@code RowSet} object that is the source of the event
   */
  void rowChanged(RowSetEvent event);

  /**
   * Notifies registered listeners that a {@code RowSet} object's
   * cursor has moved.
   * <P>
   * The source of the event can be retrieved with the method
   * {@code event.getSource}.
   *
   * @param event a {@code RowSetEvent} object that contains
   *         the {@code RowSet} object that is the source of the event
   */
  void cursorMoved(RowSetEvent event);
}
