/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * A static or instance method in the target VM. See {@link TypeComponent}
 * for general information about Field and Method mirrors.
 *
 * @see ObjectReference
 * @see ReferenceType
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface Method extends TypeComponent, Locatable, Comparable<Method> {

    /**
     * Returns a text representation of the return type,
     * as specified in the declaration of this method.
     * <P>
     * This type name is always available even if
     * the type has not yet been created or loaded.
     *
     * @return a {@code String} containing the return type name.
     */
    String returnTypeName();

    /**
     * Returns the return type,
     * as specified in the declaration of this method.
     * <P>
     * Note: if the return type of this method is a reference type (class,
     * interface, or array) and it has not been created or loaded
     * by the declaring type's class loader - that is,
     * {@link TypeComponent#declaringType declaringType()}
     * {@code .classLoader()},
     * then ClassNotLoadedException will be thrown.
     * Also, a reference type may have been loaded but not yet prepared,
     * in which case the type will be returned
     * but attempts to perform some operations on the returned type
     * (e.g. {@link ReferenceType#fields() fields()}) will throw
     * a {@link ClassNotPreparedException}.
     * Use {@link ReferenceType#isPrepared()} to determine if
     * a reference type is prepared.
     *
     * @see Type
     * @see Field#type() Field.type() - for usage examples
     * @return the return {@link Type} of this method.
     * @throws ClassNotLoadedException if the type has not yet been
     * created or loaded
     * through the appropriate class loader.
     */
    Type returnType() throws ClassNotLoadedException;

    /**
     * Returns a list containing a text representation of the type
     * of each formal parameter of this method.
     * <P>
     * This list is always available even if
     * the types have not yet been created or loaded.
     *
     * @return a {@link java.util.List List} of {@link String},
     * one List element for each parameter of this method.
     * Each element represents the type of a formal parameter
     * as specified at compile-time.
     * If the formal parameter was declared with an ellipsis, then
     * it is represented as an array of the type before the ellipsis.
     */
    List<String> argumentTypeNames();

    /**
     * Returns a list containing the type
     * of each formal parameter of this method.
     * <P>
     * Note: if there is any parameter whose type
     * is a reference type (class, interface, or array)
     * and it has not been created or loaded
     * by the declaring type's class loader - that is,
     * {@link TypeComponent#declaringType declaringType()}
     * {@code .classLoader()},
     * then ClassNotLoadedException will be thrown.
     * Also, a reference type may have been loaded but not yet prepared,
     * in which case the list will be returned
     * but attempts to perform some operations on the type
     * (e.g. {@link ReferenceType#fields() fields()}) will throw
     * a {@link ClassNotPreparedException}.
     * Use {@link ReferenceType#isPrepared()} to determine if
     * a reference type is prepared.
     *
     * @see Type
     * @return return a {@link java.util.List List} of {@link Type},
     * one List element for each parameter of this method.
     * Each element represents the type of a formal parameter
     * as specified at compile-time.
     * If the formal parameter was declared with an ellipsis, then
     * it is represented as an array of the type before the ellipsis.
     *
     * @throws ClassNotLoadedException if the type has not yet been loaded
     * through the appropriate class loader.
     */
    List<Type> argumentTypes() throws ClassNotLoadedException;

    /**
     * Determine if this method is abstract.
     *
     * @return {@code true} if the method is declared abstract;
     * {@code false} otherwise.
     */
    boolean isAbstract();

    /**
     * Determine if this method is a default method
     *
     * @return {@code true} if the method is declared default;
     * {@code false} otherwise.
     *
     * @since 1.8
     */
    default boolean isDefault() {
        throw new UnsupportedOperationException();
    }

    /**
     * Determine if this method is synchronized.
     *
     * @return {@code true} if the method is declared synchronized;
     * {@code false} otherwise.
     */
    boolean isSynchronized();

    /**
     * Determine if this method is native.
     *
     * @return {@code true} if the method is declared native;
     * {@code false} otherwise.
     */
    boolean isNative();

    /**
     * Determine if this method accepts a variable number of arguments.
     *
     * @return {@code true} if the method accepts a variable number
     * of arguments, {@code false} otherwise.
     *
     * @since 1.5
     */
    boolean isVarArgs();

    /**
     * Determine if this method is a bridge method. Bridge
     * methods are defined in
     * <cite>The Java Language Specification</cite>.
     *
     * @return {@code true} if the method is a bridge method,
     * {@code false} otherwise.
     *
     * @since 1.5
     */
    boolean isBridge();

    /**
     * Determine if this method is a constructor.
     *
     * @return {@code true} if the method is a constructor;
     * {@code false} otherwise.
     */
    boolean isConstructor();

    /**
     * Determine if this method is a static initializer.
     *
     * @return {@code true} if the method is a static initializer;
     * {@code false} otherwise.
     */
    boolean isStaticInitializer();

    /**
     * Determine if this method is obsolete.
     *
     * @return {@code true} if this method has been made obsolete by a
     * {@link VirtualMachine#redefineClasses} operation.
     *
     * @since 1.4
     */
    boolean isObsolete();

    /**
     * Returns a list containing a {@link Location} object for
     * each executable source line in this method.
     * <P>
     * This method is equivalent to
     * {@code allLineLocations(vm.getDefaultStratum(),null)} -
     * see {@link #allLineLocations(String,String)}
     * for more information.
     *
     * @return a List of all source line {@link Location} objects.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this (non-native, non-abstract)
     * method.
     */
    List<Location> allLineLocations() throws AbsentInformationException;

    /**
     * Returns a list containing a {@link Location} object for
     * each executable source line in this method.
     * <P>
     * Each location maps a source line to a range of code
     * indices.
     * The beginning of the range can be determined through
     * {@link Location#codeIndex}.
     * The returned list is ordered by code index
     * (from low to high).
     * <P>
     * The returned list may contain multiple locations for a
     * particular line number, if the compiler and/or VM has
     * mapped that line to two or more disjoint code index ranges.
     * <P>
     * If the method is native or abstract, an empty list is
     * returned.
     * <P>
     * Returned list is for the specified <i>stratum</i>
     * (see {@link Location} for a description of strata).
     *
     * @param stratum The stratum to retrieve information from
     * or {@code null} for the {@link ReferenceType#defaultStratum()}
     *
     * @param sourceName Return locations only within this
     * source file or {@code null} to return locations.
     *
     * @return a List of all source line {@link Location} objects.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this (non-native, non-abstract)
     * method.  Or if <i>sourceName</i> is non-{@code null}
     * and source name information is not present.
     *
     * @since 1.4
     */
    List<Location> allLineLocations(String stratum, String sourceName)
        throws AbsentInformationException;

    /**
     * Returns a List containing all {@link Location} objects
     * that map to the given line number.
     * <P>
     * This method is equivalent to
     * {@code locationsOfLine(vm.getDefaultStratum(), null,
     * lineNumber)} -
     * see {@link
     * #locationsOfLine(java.lang.String,java.lang.String,int)}
     * for more information.
     *
     * @param lineNumber the line number
     *
     * @return a List of {@link Location} objects that map to
     * the given line number.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this method.
     */
    List<Location> locationsOfLine(int lineNumber) throws AbsentInformationException;

    /**
     * Returns a List containing all {@link Location} objects
     * that map to the given line number and source name.
     * <P>
     * Returns a list containing each {@link Location} that maps
     * to the given line. The returned list will contain a
     * location for each disjoint range of code indices that have
     * been assigned to the given line by the compiler and/or
     * VM. Each returned location corresponds to the beginning of
     * this range.  An empty list will be returned if there is no
     * executable code at the specified line number; specifically,
     * native and abstract methods will always return an empty
     * list.
     * <p>
     * Returned list is for the specified <i>stratum</i>
     * (see {@link Location} for a description of strata).
     *
     * @param stratum the stratum to use for comparing line number
     *                and source name, or null to use the default
     *                stratum
     * @param sourceName the source name containing the
     *                   line number, or null to match all
     *                   source names
     * @param lineNumber the line number
     *
     * @return a List of {@link Location} objects that map to
     * the given line number.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this method.
     * Or if <i>sourceName</i> is non-{@code null}
     * and source name information is not present.
     *
     * @since 1.4
     */
    List<Location> locationsOfLine(String stratum, String sourceName,
                                   int lineNumber)
        throws AbsentInformationException;

    /**
     * Returns a {@link Location} for the given code index.
     *
     * @return the {@link Location} corresponding to the
     * given code index or null if the specified code index is not a
     * valid code index for this method (native and abstract methods
     * will always return null).
     */
    Location locationOfCodeIndex(long codeIndex);

    /**
     * Returns a list containing each {@link LocalVariable} declared
     * in this method. The list includes any variable declared in any
     * scope within the method. It may contain multiple variables of the
     * same name declared within disjoint scopes. Arguments are considered
     * local variables and will be present in the returned list.
     *
     * If local variable information is not available, values of
     * actual arguments to method invocations can be obtained
     * by using the method {@link StackFrame#getArgumentValues()}
     *
     * @return the list of {@link LocalVariable} objects which mirror
     * local variables declared in this method in the target VM.
     * If there are no local variables, a zero-length list is returned.
     * @throws AbsentInformationException if there is no variable
     * information for this method.
     * Generally, local variable information is not available for
     * native or abstract methods (that is, their argument name
     * information is not available), thus they will throw this exception.
     */
    List<LocalVariable> variables() throws AbsentInformationException;

    /**
     * Returns a list containing each {@link LocalVariable} of a
     * given name in this method.
     * Multiple variables can be returned
     * if the same variable name is used in disjoint
     * scopes within the method.
     *
     * @return the list of {@link LocalVariable} objects of the given
     * name.
     * If there are no matching local variables, a zero-length list
     * is returned.
     * @throws AbsentInformationException if there is no variable
     * information for this method.
     * Generally, local variable information is not available for
     * native or abstract methods (that is, their argument name
     * information is not available), thus they will throw this exception.
     */
    List<LocalVariable> variablesByName(String name)
        throws AbsentInformationException;

    /**
     * Returns a list containing each {@link LocalVariable} that is
     * declared as an argument of this method.
     *
     * If local variable information is not available, values of
     * actual arguments to method invocations can be obtained
     * by using the method {@link StackFrame#getArgumentValues()}
     *
     * @return the list of {@link LocalVariable} arguments.
     * If there are no arguments, a zero-length list is returned.
     * @throws AbsentInformationException if there is no variable
     * information for this method.
     * Generally, local variable information is not available for
     * native or abstract methods (that is, their argument name
     * information is not available), thus they will throw this exception.
     */
    List<LocalVariable> arguments() throws AbsentInformationException;

    /**
     * Returns an array containing the bytecodes for this method.
     * <P>
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetBytecodes()}
     * to determine if the operation is supported.
     *
     * @return the array of bytecodes; abstract and native methods
     * will return a zero-length array.
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support
     * the retrieval of bytecodes.
     */
    byte[] bytecodes();

    /**
     * Returns the {@link Location} of this method, if there
     * is executable code associated with it.
     *
     * @return the {@link Location} of this mirror, or null if
     * this is an abstract method; native methods will return a
     * Location object whose codeIndex is -1.
     */
    Location location();

    /**
     * Compares the specified Object with this method for equality.
     *
     * @return true if the Object is a method and if both
     * mirror the same method (declared in the same class or interface, in
     * the same VM).
     */
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this Method.
     *
     * @return the integer hash code.
     */
    int hashCode();
}
