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

/**
 * The mirror for a type in the target VM.
 * This interface is the root of a type hierarchy encompassing primitive
 * types and reference types.
 * <P>
 * A Type may be used to represent a run-time type:
 * <BLOCKQUOTE>
 *   {@link Value}.type()
 * </BLOCKQUOTE>
 * or a compile-time type:
 * <BLOCKQUOTE>
 *  {@link Field#type()} <BR>
 *  {@link Method#returnType()} <BR>
 *  {@link Method#argumentTypes()} <BR>
 *  {@link LocalVariable#type()} <BR>
 *  {@link ArrayType#componentType()}
 * </BLOCKQUOTE>
 * <P>
 * The following tables illustrate which subinterfaces of Type
 * are used to mirror types in the target VM --
 * <TABLE class="plain">
 * <CAPTION>Subinterfaces of {@link PrimitiveType}</CAPTION>
 * <THEAD style="background-color:#EEEEFF; text-align:left">
 * <TR>
 *   <TH scope="col" style="width:25em">Type declared in target as</TH>
 *   <TH scope="col" style="width:20em">Is mirrored as an instance of</TH>
 * </THEAD>
 * <TBODY style="text-align:left">
 * <TR>
 *   <TH scope="row"><CODE>boolean</CODE></TH>
 *   <TD> {@link BooleanType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>byte</CODE></TH>
 *   <TD>{@link ByteType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>char</CODE></TH>
 *   <TD>{@link CharType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>double</CODE></TH>
 *   <TD>{@link DoubleType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>float</CODE></TH>
 *   <TD>{@link FloatType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>int</CODE></TH>
 *   <TD>{@link IntegerType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>long</CODE></TH>
 *   <TD>{@link LongType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>short</CODE></TH>
 *   <TD>{@link ShortType}</TD>
 * <TR>
 *   <TH scope="row"><CODE>void</CODE></TH>
 *   <TD>{@link VoidType}</TD>
 * </TBODY>
 * </TABLE>
 *
 * <TABLE class="plain">
 * <CAPTION>Subinterfaces of {@link ReferenceType}</CAPTION>
 * <THEAD style="background-color:#EEEEFF; text-align:left">
 * <TR>
 *   <TH scope="col" style="width:15em">Type declared in target as</TH>
 *   <TH scope="col" style="width:10em">For example</TH>
 *   <TH scope="col" style="width:20em">Is mirrored as an instance of</TH>
 * </THEAD>
 * <TBODY style="text-align:left">
 * <TR>
 *   <TH scope="row"><I>a class</I></TH>
 *   <TH scope="row"><CODE>Date</CODE></TH>
 *   <TD>{@link ClassType}</TD>
 * <TR>
 *   <TH scope="row"><I>an interface</I></TH>
 *   <TH scope="row"><CODE>Runnable</CODE></TH>
 *   <TD>{@link InterfaceType}</TD>
 * <TR>
 *   <TH scope="row" rowspan="4"><I>an array</I></TH>
 *   <TH scope="row"><i>(any)</i></TH>
 *   <TD>{@link ArrayType}</TD>
 * <TR>
 *   <!--<TH scope="row"><I>an array</I></TH>-->
 *   <TH scope="row"><CODE>int[]</CODE></TH>
 *   <TD>{@link ArrayType} whose
 *         {@link ArrayType#componentType() componentType()} is
 *         {@link IntegerType}</TD>
 * <TR>
 *   <!--<TH scope="row"><I>an array</I></TH>-->
 *   <TH scope="row"><CODE>Date[]</CODE></TH>
 *   <TD>{@link ArrayType} whose
 *         {@link ArrayType#componentType() componentType()} is
 *         {@link ClassType}</TD>
 * <TR>
 *   <!--<TH scope="row"><I>an array</I></TH>-->
 *   <TH scope="row"><CODE>Runnable[]</CODE></TH>
 *   <TD>{@link ArrayType} whose
 *         {@link ArrayType#componentType() componentType()} is
 *         {@link InterfaceType}</TD>
 * </TBODY>
 * </TABLE>
 *
 * @see PrimitiveType Subinterface PrimitiveType
 * @see ReferenceType Subinterface ReferenceType
 * @see Value Value - for relationship between Type and Value
 * @see Field#type() Field.type() - for usage examples
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface Type extends Mirror {

    /**
     * Returns the type signature for this type.  The result is of the same
     * form as the string returned by {@link Class#descriptorString()}.
     * The returned string is a type descriptor conforming to JVMS {@jvms 4.3.2}
     * if this type can be described nominally.  Otherwise, the returned string
     * is not a type descriptor.
     *
     * @return the type signature
     */
    String signature();

    /**
     * Returns the name of this type. The result is of the same form as
     * the name returned by {@link Class#getName()}.
     * The returned name may not be a
     * <a href="{@docRoot}/java.base/java/lang/ClassLoader.html#binary-name">binary name</a>.
     *
     * @return the name of this type
     */
    String name();
}
