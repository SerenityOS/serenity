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

import java.util.List;
import java.util.Map;

import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.VMDisconnectEvent;

/**
 * The type of an object in a target VM. ReferenceType encompasses
 * classes, interfaces, and array types as defined in
 * <cite>The Java Language Specification</cite>.
 * All ReferenceType objects belong to one of the following
 * subinterfaces:
 * {@link ClassType} for classes,
 * {@link InterfaceType} for interfaces, and
 * {@link ArrayType} for arrays.
 * Note that primitive classes (for example, the
 * {@link ClassObjectReference#reflectedType() reflected type} of
 * {@link java.lang.Integer#TYPE Integer.TYPE})
 * are represented as ClassType.
 * The VM creates Class objects for all three, so from the VM perspective,
 * each ReferenceType maps to a distinct Class object.
 * <p>
 * ReferenceTypes can
 * be obtained by querying a particular {@link ObjectReference} for its
 * type or by getting a list of all reference types from the
 * {@link VirtualMachine}.
 * <p>
 * ReferenceType provides access to static type information such as
 * methods and fields and provides access to dynamic type
 * information such as the corresponding Class object and the classloader.
 * <p>
 * Any method on <code>ReferenceType</code> which directly or
 * indirectly takes <code>ReferenceType</code> as an parameter may throw
 * {@link VMDisconnectedException} if the target VM is
 * disconnected and the {@link VMDisconnectEvent} has been or is
 * available to be read from the {@link EventQueue}.
 * <p>
 * Any method on <code>ReferenceType</code> which directly or
 * indirectly takes <code>ReferenceType</code> as an parameter may throw
 * {@link VMOutOfMemoryException} if the target VM has run out of memory.
 * <p>
 * Any method on <code>ReferenceType</code> or which directly or indirectly takes
 * <code>ReferenceType</code> as parameter may throw
 * {@link ObjectCollectedException} if the mirrored type has been unloaded.
 *
 * @see ObjectReference
 * @see ObjectReference#referenceType
 * @see VirtualMachine
 * @see VirtualMachine#allClasses
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface ReferenceType
    extends Type, Comparable<ReferenceType>, Accessible
{
    /**
     * Returns the name of this {@code ReferenceType} object.
     * The returned name is of the same form as the name returned by
     * {@link Class#getName()}.
     *
     * @return a string containing the type name.
     * @see Class#getName()
     */
    String name();

    /**
     * Gets the generic signature for this type if there is one.
     * Generic signatures are described in the
     * <cite>The Java Virtual Machine Specification</cite>.
     *
     * @return a string containing the generic signature, or <code>null</code>
     * if there is no generic signature.
     *
     * @since 1.5
     */
    String genericSignature();

    /**
     * Gets the classloader object which loaded the class corresponding
     * to this type.
     *
     * @return a {@link ClassLoaderReference} which mirrors the classloader,
     * or <code>null</code> if the class was loaded through the bootstrap class
     * loader.
     */
    ClassLoaderReference classLoader();

    /**
     * Gets the module object which contains the class corresponding
     * to this type.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetModuleInfo()}
     * to determine if the operation is supported.
     *
     * @implSpec
     * The default implementation throws {@code UnsupportedOperationException}.
     *
     * @return a {@link ModuleReference} which mirrors the module in the target VM.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation.
     *
     * @since 9
     */
    default ModuleReference module() {
        throw new java.lang.UnsupportedOperationException(
            "The method module() must be implemented");
    }

    /**
     * Gets an identifying name for the source corresponding to the
     * declaration of this type. Interpretation of this string is
     * the responsibility of the source repository mechanism.
     * <P>
     * The returned name is dependent on VM's default stratum
     * ({@link VirtualMachine#getDefaultStratum()}).
     * In the reference implementation, when using the base stratum,
     * the returned string is the
     * unqualified name of the source file containing the declaration
     * of this type.  In other strata the returned source name is
     * the first source name for that stratum.  Since other languages
     * may have more than one source name for a reference type,
     * the use of {@link Location#sourceName()} or
     * {@link #sourceNames(String)} is preferred.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * AbsentInformationException is always thrown.
     *
     * @return the string source file name
     * @throws AbsentInformationException if the source name is not
     * known
     */
    String sourceName() throws AbsentInformationException;

    /**
     * Gets the identifying names for all the source corresponding to the
     * declaration of this type. Interpretation of these names is
     * the responsibility of the source repository mechanism.
     * <P>
     * The returned names are for the specified stratum
     * (see {@link Location} for a description of strata).
     * In the reference implementation, when using the Java
     * programming language stratum,
     * the returned List contains one element: a String which is the
     * unqualified name of the source file containing the declaration
     * of this type.  In other strata the returned source names are
     * all the source names defined for that stratum.
     *
     * @param stratum The stratum to retrieve information from
     * or <code>null</code> for the declaring type's
     * default stratum.
     *
     * @return a List of String objects each representing a source name
     *
     * @throws AbsentInformationException if the source names are not
     * known.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * AbsentInformationException is always thrown.
     *
     * @since 1.4
     */
    List<String> sourceNames(String stratum) throws AbsentInformationException;

    /**
     * Gets the paths to the source corresponding to the
     * declaration of this type. Interpretation of these paths is
     * the responsibility of the source repository mechanism.
     * <P>
     * The returned paths are for the specified stratum
     * (see {@link Location} for a description of strata).
     * In the reference implementation, for strata which
     * do not explicitly specify source path (the Java
     * programming language stratum never does), the returned
     * strings are the {@link #sourceNames(String)} prefixed by
     * the package name of this ReferenceType
     * converted to a platform dependent path.
     * For example, on a Windows platform,
     * <CODE>java.lang.Thread</CODE>
     * would return a List containing one element:
     * <CODE>"java\lang\Thread.java"</CODE>.
     *
     * @param stratum The stratum to retrieve information from
     * or <code>null</code> for the declaring type's
     * default stratum.
     *
     * @return a List of String objects each representing a source path
     *
     * @throws AbsentInformationException if the source names are not
     * known.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * AbsentInformationException is always thrown.
     *
     * @since 1.4
     */
    List<String> sourcePaths(String stratum) throws AbsentInformationException;

    /**
     * Get the source debug extension of this type.
     * <p>
     * Not all target virtual machines support this operation.
     * Use
     * {@link VirtualMachine#canGetSourceDebugExtension() canGetSourceDebugExtension()}
     * to determine if the operation is supported.
     * @return as a string the source debug extension attribute
     * @throws AbsentInformationException if the extension is not
     * specified
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetSourceDebugExtension() canGetSourceDebugExtension()},
     */
    String sourceDebugExtension() throws AbsentInformationException;

    /**
     * Determines if this type was declared static. Only nested types,
     * can be declared static, so <code>false</code> is returned
     * for any package-level type, array type, or primitive class.
     *
     * @return <code>true</code> if this type is static; false otherwise.
     */
    boolean isStatic();

    /**
     * Determines if this type was declared abstract.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * the return value is undefined.
     *
     * @return <code>true</code> if this type is abstract; false otherwise.
     */
    boolean isAbstract();

    /**
     * Determines if this type was declared final.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * the return value is always true.
     *
     * @return <code>true</code> if this type is final; false otherwise.
     */
    boolean isFinal();

    /**
     * Determines if this type has been prepared. See the JVM
     * specification for a definition of class preparation.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * the return value is undefined.
     *
     * @return <code>true</code> if this type is prepared; false otherwise.
     */
    boolean isPrepared();

    /**
     * Determines if this type has been verified. See the JVM
     * specification for a definition of class verification.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * the return value is undefined.
     *
     * @return <code>true</code> if this type is verified; false otherwise.
     */
    boolean isVerified();

    /**
     * Determines if this type has been initialized. See the JVM
     * specification for a definition of class verification.
     * For {@link InterfaceType}, this method always returns the
     * same value as {@link #isPrepared()}.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * the return value is undefined.
     *
     * @return <code>true</code> if this type is initialized; false otherwise.
     */
    boolean isInitialized();

    /**
     * Determines if initialization failed for this class. See the JVM
     * specification for details on class initialization.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes,
     * the return value is undefined.
     *
     * @return <code>true</code> if initialization was attempted and
     * failed; false otherwise.
     */
    boolean failedToInitialize();

    /**
     * Returns a list containing each {@link Field} declared in this type.
     * Inherited fields are not included. Any synthetic fields created
     * by the compiler are included in the list.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a list {@link Field} objects; the list has length 0
     * if no fields exist.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Field> fields();

    /**
     * Returns a list containing each unhidden and unambiguous {@link Field}
     * in this type.
     * Each field that can be accessed from the class
     * or its instances with its simple name is included. Fields that
     * are ambiguously multiply inherited or fields that are hidden by
     * fields with the same name in a more recently inherited class
     * cannot be accessed
     * by their simple names and are not included in the returned
     * list. All other inherited fields are included.
     * See JLS section 8.3 for details.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a List of {@link Field} objects; the list has length
     * 0 if no visible fields exist.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Field> visibleFields();

    /**
     * Returns a list containing each {@link Field} declared in this type,
     * and its superclasses, implemented interfaces, and/or superinterfaces.
     * All declared and inherited
     * fields are included, regardless of whether they are hidden or
     * multiply inherited.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a List of {@link Field} objects; the list has length
     * 0 if no fields exist.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Field> allFields();

    /**
     * Finds the visible {@link Field} with the given
     * non-ambiguous name. This method follows the
     * inheritance rules specified in the JLS (8.3.3) to determine
     * visibility.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * value is always null.
     *
     * @param fieldName a String containing the name of desired field.
     * @return a {@link Field} object which mirrors the found field, or
     * null if there is no field with the given name or if the given
     * name is ambiguous.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    Field fieldByName(String fieldName);

    /**
     * Returns a list containing each {@link Method} declared
     * directly in this type.
     * Inherited methods are not included. Constructors,
     * the initialization method if any, and any synthetic methods created
     * by the compiler are included in the list.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a list {@link Method} objects; the list has length 0
     * if no methods exist.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Method> methods();

    /**
     * Returns a list containing each {@link Method}
     * declared or inherited by this type. Methods from superclasses
     * or superinterfaces that that have been hidden or overridden
     * are not included.
     * <p>
     * Note that despite this exclusion, multiple inherited methods
     * with the same signature can be present in the returned list, but
     * at most one can be a member of a {@link ClassType}.
     * See JLS section 8.4.6 for details.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a List of {@link Method} objects; the list has length
     * 0 if no visible methods exist.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Method> visibleMethods();

    /**
     * Returns a list containing each {@link Method} declared in this type,
     * and its superclasses, implemented interfaces, and/or superinterfaces.
     * All declared and inherited
     * methods are included, regardless of whether they are hidden or
     * overridden.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a List of {@link Method} objects; the list has length
     * 0 if no methods exist.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Method> allMethods();

    /**
     * Returns a List containing each visible {@link Method} that
     * has the given name.  This is most commonly used to
     * find overloaded methods.
     * <p>
     * Overridden and hidden methods are not included.
     * See JLS (8.4.6) for details.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @param name the name of the method to find.
     * @return a List of {@link Method} objects that match the given
     * name; the list has length 0 if no matching methods are found.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Method> methodsByName(String name);

    /**
     * Returns a List containing each visible {@link Method} that
     * has the given name and signature.
     * The signature string is the
     * JNI signature for the target method:
     * <ul>
     * <li><code>()V</code>
     * <li><code>([Ljava/lang/String;)V</code>
     * <li><code>(IIII)Z</code>
     * </ul>
     * This method follows the inheritance rules specified
     * in the JLS (8.4.6) to determine visibility.
     * <p>
     * At most one method in the list is a concrete method and a
     * component of {@link ClassType}; any other methods in the list
     * are abstract. Use {@link ClassType#concreteMethodByName} to
     * retrieve only the matching concrete method.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @param name the name of the method to find.
     * @param signature the signature of the method to find
     * @return a List of {@link Method} objects that match the given
     * name and signature; the list has length 0 if no matching methods
     * are found.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<Method> methodsByName(String name, String signature);

    /**
     * Returns a List containing {@link ReferenceType} objects that are
     * declared within this type and are currently loaded into the Virtual
     * Machine.  Both static nested types and non-static nested
     * types (that is, inner types) are included. Local inner types
     * (declared within a code block somewhere in this reference type) are
     * also included in the returned list.
     * <p>
     * For arrays ({@link ArrayType}) and primitive classes, the returned
     * list is always empty.
     *
     * @return a List of nested {@link ReferenceType} objects; the list
     * has 0 length if there are no nested types.
     */
    List<ReferenceType> nestedTypes();

    /**
     * Gets the {@link Value} of a given static {@link Field} in this type.
     * The Field must be valid for this type;
     * that is, it must be declared in this type, a superclass, a
     * superinterface, or an implemented interface.
     *
     * @param field the field containing the requested value
     * @return the {@link Value} of the instance field.
     * @throws java.lang.IllegalArgumentException if the field is not valid for
     * this object's class.
     */
    Value getValue(Field field);

    /**
     * Returns a map containing the {@link Value} of each
     * static {@link Field} in the given list.
     * The Fields must be valid for this type;
     * that is, they must be declared in this type, a superclass, a
     * superinterface, or an implemented interface.
     *
     * @param fields a list of {@link Field} objects containing the
     * requested values.
     * @return a Map of the requested {@link Field} objects with
     * their {@link Value}.
     * @throws java.lang.IllegalArgumentException if any field is not valid for
     * this object's class.
     * @throws VMMismatchException if a {@link Mirror} argument and this mirror
     * do not belong to the same {@link VirtualMachine}.
     */
    Map<Field,Value> getValues(List<? extends Field> fields);

    /**
     * Returns the class object that corresponds to this type in the
     * target VM. The VM creates class objects for every kind of
     * ReferenceType: classes, interfaces, and array types.
     * @return the {@link ClassObjectReference} for this reference type
     * in the target VM.
     */
    ClassObjectReference classObject();

    /**
     * Returns a list containing a {@link Location} object
     * for each executable source line in this reference type.
     * <P>
     * This method is equivalent to
     * <code>allLineLocations(vm.getDefaultStratum(),null)</code> -
     * see {@link #allLineLocations(String,String)}
     * for more information.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this class and there are non-native,
     * non-abstract executable members of this class.
     *
     * @throws ClassNotPreparedException if this class not yet
     * been prepared.
     */
    List<Location> allLineLocations() throws AbsentInformationException;

    /**
     * Returns a list containing a {@link Location} object
     * for each executable source line in this reference type.
     * Each location maps a source line to a range of code
     * indices.
     * The beginning of the range can be determined through
     * {@link Location#codeIndex}.  The returned list may contain
     * multiple locations for a particular line number, if the
     * compiler and/or VM has mapped that line to two or more
     * disjoint code index ranges.  Note that it is possible for
     * the same source line to represent different code index
     * ranges in <i>different</i> methods.
     * <P>
     * For arrays ({@link ArrayType}) and primitive classes, the
     * returned list is always empty.  For interfaces ({@link
     * InterfaceType}), the returned list will be non-empty only
     * if the interface has executable code in its class
     * initialization.
     * <P>
     * Returned list is for the specified <i>stratum</i>
     * (see {@link Location} for a description of strata).
     *
     * @param stratum The stratum to retrieve information from
     * or <code>null</code> for the {@link #defaultStratum()}.
     *
     * @param sourceName Return locations only within this
     * source file or <code>null</code> to return locations.
     *
     * @return a List of all source line {@link Location} objects.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this class and there are non-native,
     * non-abstract executable members of this class.
     * Or if <i>sourceName</i> is non-<code>null</code>
     * and source name information is not present.
     *
     * @throws ClassNotPreparedException if this class not yet
     * been prepared.
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
     * <code>locationsOfLine(vm.getDefaultStratum(), null,
     * lineNumber)</code> -
     * see {@link
     * #locationsOfLine(java.lang.String,java.lang.String,int)}
     * for more information.
     *
     * @param lineNumber the line number
     *
     * @return a List of all {@link Location} objects that map to
     * the given line.
     *
     * @throws AbsentInformationException if there is no line
     * number information for this class.
     *
     * @throws ClassNotPreparedException if this class not yet
     * been prepared.
     *
     * @see VirtualMachine#getDefaultStratum()
     */
    List<Location> locationsOfLine(int lineNumber)
        throws AbsentInformationException;

    /**
     * Returns a List containing all {@link Location} objects
     * that map to the given line number.
     * <P>
     * For arrays ({@link ArrayType}) and primitive classes, the
     * returned list is always empty.
     * For interfaces ({@link InterfaceType}), the returned list
     * will be non-empty only if the interface has executable code
     * in its class initialization at the specified line number.
     * An empty list will be returned if there is no executable
     * code at the specified line number.
     * <p>
     * Returned list is for the specified <i>stratum</i>
     * (see {@link Location} for a description of strata).
     *
     * @param stratum the stratum to use for comparing line number
     *                and source name, or <code>null</code> to
     *                use the {@link #defaultStratum()}.
     *
     * @param sourceName the source name containing the line
     *                   number, or <code>null</code> to match
     *                   all source names
     *
     * @param lineNumber the line number
     *
     * @return a List of all {@link Location} objects that map
     *         to the given line.
     *
     * @throws AbsentInformationException if there is no line
     *         number information for this class.
     *         Or if <i>sourceName</i> is non-<code>null</code>
     *         and source name information is not present.
     *
     * @throws ClassNotPreparedException if this class not yet
     *         been prepared.
     *
     * @since 1.4
     */
    List<Location> locationsOfLine(String stratum,
                                   String sourceName,
                                   int lineNumber)
        throws AbsentInformationException;

    /**
     * Return the available strata for this reference type.
     * <P>
     * See the {@link Location} for a description of strata.
     *
     * @return List of <CODE>java.lang.String</CODE>, each
     * representing a stratum
     *
     * @since 1.4
     */
    List<String> availableStrata();

    /**
     * Returns the default stratum for this reference type.
     * This value is specified in the class file and cannot
     * be set by the user.  If the class file does not
     * specify a default stratum the base stratum
     * (<code>"Java"</code>) will be returned.
     * <P>
     * See the {@link Location} for a description of strata.
     *
     * @since 1.4
     */
    String defaultStratum();

    /**
     * Returns instances of this ReferenceType.
     * Only instances that are reachable for the purposes of garbage collection
     * are returned.
     * <p>
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetInstanceInfo()}
     * to determine if the operation is supported.
     *
     * @see VirtualMachine#instanceCounts(List)
     * @see ObjectReference#referringObjects(long)
     *
     * @param maxInstances the maximum number of instances to return.
     *        Must be non-negative.  If zero, all instances are returned.
     * @return a List of {@link ObjectReference} objects.  If there are
     * no instances of this ReferenceType, a zero-length list is returned.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetInstanceInfo() canGetInstanceInfo()}
     * @throws java.lang.IllegalArgumentException if maxInstances is less
     *         than zero.
     * @since 1.6
     */
    List<ObjectReference> instances(long maxInstances);

    /**
     * Compares the specified Object with this ReferenceType for equality.
     *
     * @return  true if the Object is a {@link ReferenceType}, if the
     * ReferenceTypes belong to the same VM, and if they mirror classes
     * which correspond to the same instance of java.lang.Class in that VM.
     */
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this ObjectReference.
     *
     * @return the integer hash code
     */
    int hashCode();

    /**
     * Returns the class major version number, as defined in the class file format
     * of the Java Virtual Machine Specification.
     *
     * For arrays ({@link ArrayType}) and primitive classes,
     * the returned major version number value is zero.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetClassFileVersion()}
     * to determine if the operation is supported.
     *
     * @return the major version number of the class.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetClassFileVersion() canGetClassFileVersion()}
     *
     * @since 1.6
     */
    int majorVersion();

    /**
     * Returns the class minor version number, as defined in the class file format
     * of the Java Virtual Machine Specification.
     *
     * For arrays ({@link ArrayType}) and primitive classes,
     * the returned minor version number value is zero.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetClassFileVersion()}
     * to determine if the operation is supported.
     *
     * @return the minor version number of the class.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetClassFileVersion() canGetClassFileVersion()}
     *
     * @since 1.6
     */
    int minorVersion();

    /**
     * Returns the number of entries in the constant pool plus one.
     * This corresponds to the constant_pool_count item of the Class File Format
     * in the Java Virtual Machine Specification.
     *
     * For arrays ({@link ArrayType}) and primitive classes,
     * the returned constant pool count value is zero.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetConstantPool()}
     * to determine if the operation is supported.
     *
     * @return total number of constant pool entries for a class plus one.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetConstantPool() canGetConstantPool()}
     *
     * @see #constantPool()
     * @since 1.6
     */
    int constantPoolCount();

    /**
     * Returns the raw bytes of the constant pool in the format of the
     * constant_pool item of the Class File Format in the Java Virtual
     * Machine Specification. The format of the constant pool may
     * differ between versions of the Class File Format, so, the
     * minor and major class version numbers should be checked for
     * compatibility.
     *
     * For arrays ({@link ArrayType}) and primitive classes,
     * a zero length byte array is returned.
     *
     * Not all target virtual machines support this operation.
     * Use {@link VirtualMachine#canGetConstantPool()}
     * to determine if the operation is supported.
     *
     * @return the raw bytes of constant pool.
     *
     * @throws java.lang.UnsupportedOperationException if
     * the target virtual machine does not support this
     * operation - see
     * {@link VirtualMachine#canGetConstantPool() canGetConstantPool()}
     *
     * @see #constantPoolCount()
     * @since 1.6
     */
     byte[] constantPool();
}
