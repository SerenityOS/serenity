/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Drag and Drop is a direct manipulation gesture found in many Graphical User
 * Interface systems that provides a mechanism to transfer information between
 * two entities logically associated with presentation elements in the GUI.
 * Normally driven by a physical gesture of a human user using an appropriate
 * input device, Drag and Drop provides both a mechanism to enable continuous
 * feedback regarding the possible outcome of any subsequent data transfer to
 * the user during navigation over the presentation elements in the GUI, and the
 * facilities to provide for any subsequent data negotiation and transfer.
 * <p>
 * This package defines the classes and interfaces necessary to perform Drag and
 * Drop operations in Java. It defines classes for the drag-source and the
 * drop-target, as well as events for transferring the data being dragged. This
 * package also provides a means for giving visual feedback to the user
 * throughout the duration of the Drag and Drop operation.
 * <p>
 * A typical Drag and Drop operation can be decomposed into the following states
 * (not entirely sequentially):
 * <ul>
 *     <li>A {@code DragSource} comes into existence, associated with some
 *     presentation element ({@code Component}) in the GUI, to initiate a Drag
 *     and Drop of some potentially {@code Transferable} data.</li>
 *     <li>1 or more {@code DropTarget}(s) come into/go out of existence,
 *     associated with presentation elements in the GUI (Components),
 *     potentially capable of consuming {@code Transferable} data types.</li>
 *     <li>A {@code DragGestureRecognizer} is obtained from the
 *     {@code DragSource} and is associated with a {@code Component} in order to
 *     track and identify any Drag initiating gesture by the user over the
 *     {@code Component}.</li>
 *     <li>A user makes a Drag gesture over the {@code Component}, which the
 *     registered {@code DragGestureRecognizer} detects, and notifies its
 *     {@code DragGestureListener} of.
 *     <p>
 *     Note: Although this API consistently refers to the stimulus for a drag
 *     and drop operation being a physical gesture by a human user, this does
 *     not preclude a programmatically driven DnD operation given the
 *     appropriate implementation of a {@code DragSource}. This package
 *     contains the abstract class {@code MouseDragGestureRecognizer} for
 *     recognizing mouse device gestures. Other abstract subclasses may be
 *     provided by the platform to support other input devices or particular
 *     {@code Component} class semantics.</li>
 *     <li>The {@code DragGestureListener} causes the {@code DragSource} to
 *     initiate the Drag and Drop operation on behalf of the user, perhaps
 *     animating the GUI Cursor and/or rendering an {@code Image} of the item(s)
 *     that are the subject of the operation.</li>
 *     <li>As the user gestures navigate over {@code Component}(s) in the GUI
 *     with associated {@code DropTarget}(s), the {@code DragSource} receives
 *     notifications in order to provide "Drag Over" feedback effects, and the
 *     {@code DropTarget}(s) receive notifications in order to provide
 *     "Drag Under" feedback effects based upon the operation(s) supported and
 *     the data type(s) involved.</li>
 * </ul>
 * <p>
 * The gesture itself moves a logical cursor across the GUI hierarchy,
 * intersecting the geometry of GUI Component(s), possibly resulting in the
 * logical "Drag" cursor entering, crossing, and subsequently leaving
 * {@code Component}(s) and associated {@code DropTarget}(s).
 * <p>
 * The {@code DragSource} object manifests "Drag Over" feedback to the user, in
 * the typical case by animating the GUI {@code Cursor} associated with the
 * logical cursor.
 * <p>
 * {@code DropTarget} objects manifest "Drag Under" feedback to the user, in the
 * typical case, by rendering animations into their associated GUI
 * {@code Component}(s) under the GUI Cursor.
 * <p>
 * The determination of the feedback effects, and the ultimate success or
 * failure of the data transfer, should one occur, is parameterized as follows:
 * <ul>
 *     <li>By the transfer "operation" selected by the user, and supported by
 *     both the {@code DragSource} and {@code DropTarget}: Copy, Move or
 *     Reference(link).</li>
 *     <li>By the intersection of the set of data types provided by the
 *     {@code DragSource} and the set of data types comprehensible by the
 *     {@code DropTarget}.</li>
 *     <li>When the user terminates the drag operation, normally resulting in a
 *     successful Drop, both the {@code DragSource} and {@code DropTarget}
 *     receive notifications that include, and result in the type negotiation
 *     and transfer of, the information associated with the {@code DragSource}
 *     via a {@code Transferable} object.</li>
 * </ul>
 *
 * @since 1.2
 */
package java.awt.dnd;
