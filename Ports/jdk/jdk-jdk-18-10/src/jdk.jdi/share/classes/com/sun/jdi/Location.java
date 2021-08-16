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

import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.request.EventRequestManager;

/**
 * A point within the executing code of the target VM.
 * Locations are used to identify the current position of
 * a suspended thread (analogous to an instruction pointer or
 * program counter register in native programs). They are also used
 * to identify the position at which to set a breakpoint.
 * <p>
 * The availability of a line number for a location will
 * depend on the level of debugging information available from the
 * target VM.
 * <p>
 * Several mirror interfaces have locations. Each such mirror
 * extends a {@link Locatable} interface.
 * <p>
 * <a id="strata"><b>Strata</b></a>
 * <p>
 * The source information for a Location is dependent on the
 * <i>stratum</i> which is used. A stratum is a source code
 * level within a sequence of translations.  For example,
 * say the baz program is written in the programming language
 * "Foo" then translated to the language "Bar" and finally
 * translated into the Java programming language.  The
 * Java programming language stratum is named
 * <code>"Java"</code>, let's say the other strata are named
 * "Foo" and "Bar".  A given location (as viewed by the
 * {@link #sourceName()} and {@link #lineNumber()} methods)
 * might be at line 14 of "baz.foo" in the <code>"Foo"</code>
 * stratum, line 23 of "baz.bar" in the <code>"Bar"</code>
 * stratum and line 71 of the <code>"Java"</code> stratum.
 * Note that while the Java programming language may have
 * only one source file for a reference type, this restriction
 * does not apply to other strata - thus each Location should
 * be consulted to determine its source path.
 * Queries which do not specify a stratum
 * ({@link #sourceName()}, {@link #sourcePath()} and
 * {@link #lineNumber()}) use the VM's default stratum
 * ({@link VirtualMachine#getDefaultStratum()}).
 * If the specified stratum (whether explicitly specified
 * by a method parameter or implicitly as the VM's default)
 * is <code>null</code> or is not available in the declaring
 * type, the declaring type's default stratum is used
 * ({@link #declaringType()}.{@link ReferenceType#defaultStratum()
 * defaultStratum()}).  Note that in the normal case, of code
 * that originates as Java programming language source, there
 * will be only one stratum (<code>"Java"</code>) and it will be
 * returned as the default.  To determine the available strata
 * use {@link ReferenceType#availableStrata()}.
 *
 * @see EventRequestManager
 * @see StackFrame
 * @see BreakpointEvent
 * @see ExceptionEvent
 * @see Locatable
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since 1.3
 */
public interface Location extends Mirror, Comparable<Location> {

    /**
     * Gets the type to which this Location belongs. Normally
     * the declaring type is a {@link ClassType}, but executable
     * locations also may exist within the static initializer of an
     * {@link InterfaceType}.
     *
     * @return the {@link ReferenceType} containing this Location.
     */
    ReferenceType declaringType();

    /**
     * Gets the method containing this Location.
     *
     * @return the location's {@link Method}.
     */
    Method method();

    /**
     * Gets the code position within this location's method.
     *
     * @return the long representing the position within the method
     * or -1 if location is within a native method.
     */
    long codeIndex();

    /**
     * Gets an identifing name for the source corresponding to
     * this location.
     * <P>
     * This method is equivalent to
     * <code>sourceName(vm.getDefaultStratum())</code> -
     * see {@link #sourceName(String)}
     * for more information.
     *
     * @return a string specifying the source
     * @throws AbsentInformationException if the source name is not
     * known
     */
    String sourceName() throws AbsentInformationException;

    /**
     * Gets an identifing name for the source corresponding to
     * this location. Interpretation of this string is the
     * responsibility of the source repository mechanism.
     * <P>
     * Returned name is for the specified <i>stratum</i>
     * (see the {@link Location class comment} for a
     * description of strata).
     * <P>
     * The returned string is the unqualified name of the source
     * file for this Location.  For example,
     * <CODE>java.lang.Thread</CODE> would return
     * <CODE>"Thread.java"</CODE>.
     *
     * @param stratum The stratum to retrieve information from
     * or <code>null</code> for the declaring type's
     * default stratum.
     *
     * @return a string specifying the source
     *
     * @throws AbsentInformationException if the source name is not
     * known
     *
     * @since 1.4
     */
    String sourceName(String stratum) throws AbsentInformationException;

    /**
     * Gets the path to the source corresponding to this
     * location.
     * <P>
     * This method is equivalent to
     * <code>sourcePath(vm.getDefaultStratum())</code> -
     * see {@link #sourcePath(String)}
     * for more information.
     *
     * @return a string specifying the source
     *
     * @throws AbsentInformationException if the source name is not
     * known
     */
    String sourcePath() throws AbsentInformationException;

    /**
     * Gets the path to the source corresponding to this
     * location. Interpretation of this string is the
     * responsibility of the source repository mechanism.
     * <P>
     * Returned path is for the specified <i>stratum</i>
     * (see the {@link Location class comment} for a
     * description of strata).
     * <P>
     * In the reference implementation, for strata which
     * do not explicitly specify source path (the Java
     * programming language stratum never does), the returned
     * string is the package name of {@link #declaringType()}
     * converted to a platform dependent path followed by the
     * unqualified name of the source file for this Location
     * ({@link #sourceName sourceName(stratum)}).
     * For example, on a
     * Windows platform, <CODE>java.lang.Thread</CODE>
     * would return
     * <CODE>"java\lang\Thread.java"</CODE>.
     *
     * @param stratum The stratum to retrieve information from
     * or <code>null</code> for the declaring type's
     * default stratum.
     *
     * @return a string specifying the source
     *
     * @throws AbsentInformationException if the source name is not
     * known
     *
     * @since 1.4
     */
    String sourcePath(String stratum) throws AbsentInformationException;

    /**
     * Gets the line number of this Location.
     * <P>
     * This method is equivalent to
     * <code>lineNumber(vm.getDefaultStratum())</code> -
     * see {@link #lineNumber(String)}
     * for more information.
     *
     * @return an int specifying the line in the source, returns
     * -1 if the information is not available; specifically, always
     * returns -1 for native methods.
     */
    int lineNumber();

    /**
     * The line number of this Location.  The line number is
     * relative to the source specified by
     * {@link #sourceName(String) sourceName(stratum)}.
     * <P>
     * Returned line number is for the specified <i>stratum</i>
     * (see the {@link Location class comment} for a
     * description of strata).
     *
     * @param stratum The stratum to retrieve information from
     * or <code>null</code> for the declaring type's
     * default stratum.
     *
     * @return an int specifying the line in the source, returns
     * -1 if the information is not available; specifically, always
     * returns -1 for native methods.
     *
     * @since 1.4
     */
    int lineNumber(String stratum);

    /**
     * Compares the specified Object with this Location for equality.
     *
     * @return true if the Object is a Location and if it refers to
     * the same point in the same VM as this Location.
     */
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this Location.
     *
     * @return the integer hash code
     */
    int hashCode();
}
