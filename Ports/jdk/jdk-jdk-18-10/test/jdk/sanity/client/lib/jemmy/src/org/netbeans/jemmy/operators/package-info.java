/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
 * <h1>Operators package</h1>
 * Contains so called "operators".<br><br>
 * <i>Operator</i> is a <i>test-side representation</i> for a component. Each
 * particular operator class provides all the functionality to work with one
 * component type. For example {@code JButtonOperator} covers
 * {@code javax.swing.JButton}.<br><br>
 * Operators inheritance tree exactly matches component types inheritance:
 * {@code AbstractButton} extending {@code JComponent} means that
 * {@code AbstractBittonOperator} extends
 * {@code JComponentOperator}.<br><br>
 * Every operator provides, basicly, all the methods to reproduce user actions
 * which can be performed on a component type covered by operator.<br><br>
 * Every operator also provides <i>mapping</i> functionality: API to invoke
 * component method during the event queue. For example,
 * {@code AbstractButtonOperator} has {@code getText()} method which
 * simply invokes {@code AbstractButton.getText()} through the
 * queue.<br><br>
 *
 * @since 23 Feb 2002
 * <hr>
 */
package org.netbeans.jemmy.operators;
