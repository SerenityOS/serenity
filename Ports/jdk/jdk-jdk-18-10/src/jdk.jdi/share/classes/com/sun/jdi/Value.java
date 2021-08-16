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

package com.sun.jdi;

import com.sun.jdi.event.ModificationWatchpointEvent;

/**
 * The mirror for a value in the target VM.
 * This interface is the root of a
 * value hierarchy encompassing primitive values and object values.
 * <P>
 * Some examples of where values may be accessed:
 * <BLOCKQUOTE><TABLE role="presentation">
 * <TR>
 *   <TD>{@link ObjectReference#getValue(Field)
 *                 ObjectReference.getValue(Field)}
 *   <TD>- value of a field
 * <TR>
 *   <TD>{@link StackFrame#getValue(LocalVariable)
 *                 StackFrame.getValue(LocalVariable)}
 *   <TD>- value of a variable
 * <TR>
 *   <TD>{@link VirtualMachine#mirrorOf(double)
 *                 VirtualMachine.mirrorOf(double)}
 *   <TD>- created in the target VM by the JDI client
 * <TR>
 *   <TD>{@link ModificationWatchpointEvent#valueToBe()
 *                 ModificationWatchpointEvent.valueToBe()}
 *   <TD>- returned with an event
 * </TABLE></BLOCKQUOTE>
 * <P>
 * The following tables illustrate which subinterfaces of Value
 * are used to mirror values in the target VM --
 * <TABLE class="plain">
 * <CAPTION>Subinterfaces of {@link PrimitiveValue}</CAPTION>
 * <THEAD style="background-color:#EEEEFF; text-align:left">
 * <TR>
 *   <TH scope="col" style="width:10em">Kind of value</TH>
 *   <TH scope="col" style="width:15em">For example -<br>expression in target</TH>
 *   <TH scope="col" style="width:15em">Is mirrored as an<br>instance of</TH>
 *   <TH scope="col" style="width:15em">{@link Type} of value<br>{@link #type() Value.type()}</TH>
 * </THEAD>
 * <TBODY style="text-align:left">
 * <TR>
 *   <TH scope="row">a boolean</TH>
 *   <TD>{@code true}</TD>
 *   <TD>{@link BooleanValue}</TD>
 *   <TD>{@link BooleanType}</TD>
 * <TR>
 *   <TH scope="row">a byte</TH>
 *   <TD>{@code (byte)4}</TD>
 *   <TD>{@link ByteValue}</TD>
 *   <TD>{@link ByteType}</TD>
 * <TR>
 *   <TH scope="row">a char</TH>
 *   <TD>{@code 'a'}</TD>
 *   <TD>{@link CharValue}</TD>
 *   <TD>{@link CharType}</TD>
 * <TR>
 *   <TH scope="row">a double</TH>
 *   <TD>{@code 3.1415926}</TD>
 *   <TD>{@link DoubleValue}</TD>
 *   <TD>{@link DoubleType}</TD>
 * <TR>
 *   <TH scope="row">a float</TH>
 *   <TD>{@code 2.5f}</TD>
 *   <TD>{@link FloatValue}</TD>
 *   <TD>{@link FloatType}</TD>
 * <TR>
 *   <TH scope="row">an int</TH>
 *   <TD>{@code 22}</TD>
 *   <TD>{@link IntegerValue}</TD>
 *   <TD>{@link IntegerType}</TD>
 * <TR>
 *   <TH scope="row">a long</TH>
 *   <TD>{@code 1024L}</TD>
 *   <TD>{@link LongValue}</TD>
 *   <TD>{@link LongType}</TD>
 * <TR>
 *   <TH scope="row">a short</TH>
 *   <TD>{@code (short)12}</TD>
 *   <TD>{@link ShortValue}</TD>
 *   <TD>{@link ShortType}</TD>
 * <TR>
 *   <TH scope="row">a void</TH>
 *   <TD></TD>
 *   <TD>{@link VoidValue}</TD>
 *   <TD>{@link VoidType}</TD>
 * </TBODY>
 * </TABLE>
 *
 * <TABLE class="plain">
 * <CAPTION>Subinterfaces of {@link ObjectReference}</CAPTION>
 * <THEAD style="background-color:#EEEEFF; text-align:left">
 * <TR>
 *   <TH scope="col" style="width:10em">Kind of value</TH>
 *   <TH scope="col" style="width:15em">For example -<br>expression in target</TH>
 *   <TH scope="col" style="width:15em">Is mirrored as an<br>instance of</TH>
 *   <TH scope="col" style="width:15em">{@link Type} of value<br>{@link #type() Value.type()}</TH>
 * </THEAD>
 * <TBODY style="text-align:left">
 * <TR>
 *   <TH scope="row">a class instance</TH>
 *   <TD>{@code this}</TD>
 *   <TD>{@link ObjectReference}</TD>
 *   <TD>{@link ClassType}</TD>
 * <TR>
 *   <TH scope="row">an array</TH>
 *   <TD>{@code new int[5]}</TD>
 *   <TD>{@link ArrayReference}</TD>
 *   <TD>{@link ArrayType}</TD>
 * <TR>
 *   <TH scope="row">a string</TH>
 *   <TD>{@code "hello"}</TD>
 *   <TD>{@link StringReference}</TD>
 *   <TD>{@link ClassType}</TD>
 * <TR>
 *   <TH scope="row">a thread</TH>
 *   <TD>{@code Thread.currentThread()}</TD>
 *   <TD>{@link ThreadReference}</TD>
 *   <TD>{@link ClassType}</TD>
 * <TR>
 *   <TH scope="row">a thread group</TH>
 *   <TD>{@code Thread.currentThread()}<br>&nbsp;&nbsp;{@code .getThreadGroup()}</TD>
 *   <TD>{@link ThreadGroupReference}</TD>
 *   <TD>{@link ClassType}</TD>
 * <TR>
 *   <TH scope="row">a {@code java.lang.Class}<br>instance</TH>
 *   <TD>{@code this.getClass()}</TD>
 *   <TD>{@link ClassObjectReference}</TD>
 *   <TD>{@link ClassType}</TD>
 * <TR>
 *   <TH scope="row">a class loader</TH>
 *   <TD>{@code this.getClass()}<br>&nbsp;&nbsp;{@code .getClassLoader()}</TD>
 *   <TD>{@link ClassLoaderReference}</TD>
 *   <TD>{@link ClassType}</TD>
 * </TBODY>
 * </TABLE>
 *
 * <TABLE class="plain">
 * <CAPTION>Other values</CAPTION>
 * <THEAD style="background-color:#EEEEFF; text-align:left">
 * <TR>
 *   <TH scope="col" style="width:10em">Kind of value</TH>
 *   <TH scope="col" style="width:15em">For example -<br>expression in target</TH>
 *   <TH scope="col" style="width:15em">Is mirrored as</TH>
 *   <TH scope="col" style="width:15em">{@link Type} of value</TH>
 * </THEAD>
 * <TBODY style="text-align:left">
 * <TR>
 *   <TH scope="row">null</TH>
 *   <TD>{@code null}</TD>
 *   <TD>{@code null}</TD>
 *   <TD>n/a</TD>
 * </TBODY>
 * </TABLE>
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */

public interface Value extends Mirror {

    /**
     * Returns the run-time type of this value.
     *
     * @see Type
     * @return a {@link Type} which mirrors the value's type in the
     * target VM.
     */
    Type type();
}
