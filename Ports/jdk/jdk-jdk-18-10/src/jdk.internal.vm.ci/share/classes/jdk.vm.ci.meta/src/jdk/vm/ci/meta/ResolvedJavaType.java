/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.vm.ci.meta;

import java.lang.reflect.AnnotatedElement;

import jdk.vm.ci.meta.Assumptions.AssumptionResult;

/**
 * Represents a resolved Java type. Types include primitives, objects, {@code void}, and arrays
 * thereof. Types, like fields and methods, are resolved through {@link ConstantPool constant pools}
 * .
 */
public interface ResolvedJavaType extends JavaType, ModifiersProvider, AnnotatedElement {
    /**
     * Checks whether this type has a finalizer method.
     *
     * @return {@code true} if this class has a finalizer
     */
    boolean hasFinalizer();

    /**
     * Checks whether this type has any finalizable subclasses so far. Any decisions based on this
     * information require the registration of a dependency, since this information may change.
     *
     * @return {@code true} if this class has any subclasses with finalizers
     */
    AssumptionResult<Boolean> hasFinalizableSubclass();

    /**
     * Checks whether this type is an interface.
     *
     * @return {@code true} if this type is an interface
     */
    @Override
    boolean isInterface();

    /**
     * Checks whether this type is an instance class.
     *
     * @return {@code true} if this type is an instance class
     */
    boolean isInstanceClass();

    /**
     * Checks whether this type is primitive.
     *
     * @return {@code true} if this type is primitive
     */
    boolean isPrimitive();

    /*
     * The setting of the final bit for types is a bit confusing since arrays are marked as final.
     * This method provides a semantically equivalent test that appropriate for types.
     */
    default boolean isLeaf() {
        return getElementalType().isFinalFlagSet();
    }

    /**
     * Checks whether this type is an enum.
     *
     * @return {@code true} if this type is an enum
     */
    boolean isEnum();

    /**
     * Checks whether this type is initialized. If a type is initialized it implies that it was
     * {@link #isLinked() linked} and that the static initializer has run.
     *
     * @return {@code true} if this type is initialized
     */
    boolean isInitialized();

    /**
     * Initializes this type.
     */
    void initialize();

    /**
     * Checks whether this type is linked and verified. When a type is linked the static initializer
     * has not necessarily run. An {@link #isInitialized() initialized} type is always linked.
     *
     * @return {@code true} if this type is linked
     */
    boolean isLinked();

    /**
     * Links this type. If this method returns normally, then future calls of {@link #isLinked} will
     * return true and future calls of {@link #link} are no-ops. If the method throws an exception,
     * then future calls of {@link #isLinked} will return false and future calls of {@link #link}
     * will reattempt the linking step which might succeed or throw an exception.
     */
    default void link() {
        throw new UnsupportedOperationException("link is unsupported");
    }

    /**
     * Checks whether this type or any of its supertypes or superinterfaces has default methods.
     */
    default boolean hasDefaultMethods() {
        throw new UnsupportedOperationException("hasDefaultMethods is unsupported");
    }

    /**
     * Checks whether this type declares defaults methods.
     */
    default boolean declaresDefaultMethods() {
        throw new UnsupportedOperationException("declaresDefaultMethods is unsupported");
    }

    /**
     * Determines if this type is either the same as, or is a superclass or superinterface of, the
     * type represented by the specified parameter. This method is identical to
     * {@link Class#isAssignableFrom(Class)} in terms of the value return for this type.
     */
    boolean isAssignableFrom(ResolvedJavaType other);

    /**
     * Returns {@code null} since support for VM anonymous class was removed by JDK-8243287.
     * This method is preserved for JVMCI backwards compatibility.
     */
    @Deprecated
    default ResolvedJavaType getHostClass() {
        return null;
    }

    /**
     * Returns true if this type is exactly the type {@link java.lang.Object}.
     */
    default boolean isJavaLangObject() {
        // Removed assertion due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=434442
        return getSuperclass() == null && !isInterface() && getJavaKind() == JavaKind.Object;
    }

    /**
     * Checks whether the specified object is an instance of this type.
     *
     * @param obj the object to test
     * @return {@code true} if the object is an instance of this type
     */
    boolean isInstance(JavaConstant obj);

    /**
     * Gets the super class of this type. If this type represents either the {@code Object} class,
     * an interface, a primitive type, or void, then null is returned. If this object represents an
     * array class then the type object representing the {@code Object} class is returned.
     */
    ResolvedJavaType getSuperclass();

    /**
     * Gets the interfaces implemented or extended by this type. This method is analogous to
     * {@link Class#getInterfaces()} and as such, only returns the interfaces directly implemented
     * or extended by this type.
     */
    ResolvedJavaType[] getInterfaces();

    /**
     * Gets the single implementor of this type. Calling this method on a non-interface type causes
     * an exception.
     * <p>
     * If the compiler uses the result of this method for its compilation, the usage must be guarded
     * because the verifier can not guarantee that the assigned type really implements this
     * interface. Additionally, class loading can invalidate the result of this method.
     *
     * @return {@code null} if there is no implementor, the implementor if there is only one, or
     *         {@code this} if there are more than one.
     */
    ResolvedJavaType getSingleImplementor();

    /**
     * Walks the class hierarchy upwards and returns the least common class that is a superclass of
     * both the current and the given type.
     *
     * @return the least common type that is a super type of both the current and the given type, or
     *         {@code null} if primitive types are involved.
     */
    ResolvedJavaType findLeastCommonAncestor(ResolvedJavaType otherType);

    /**
     * Attempts to get a leaf concrete subclass of this type.
     * <p>
     * For an {@linkplain #isArray() array} type A, the leaf concrete subclass is A if the
     * {@linkplain #getElementalType() elemental} type of A is final (which includes primitive
     * types). Otherwise {@code null} is returned for A.
     * <p>
     * For a non-array type T, the result is the leaf concrete type in the current hierarchy of T.
     * <p>
     * A runtime may decide not to manage or walk a large hierarchy and so the result is
     * conservative. That is, a non-null result is guaranteed to be the leaf concrete class in T's
     * hierarchy <b>at the current point in time</b> but a null result does not necessarily imply
     * that there is no leaf concrete class in T's hierarchy.
     * <p>
     * If the compiler uses the result of this method for its compilation, it must register the
     * {@link AssumptionResult} in its {@link Assumptions} because dynamic class loading can
     * invalidate the result of this method.
     *
     * @return an {@link AssumptionResult} containing the leaf concrete subclass for this type as
     *         described above
     */
    AssumptionResult<ResolvedJavaType> findLeafConcreteSubtype();

    @Override
    ResolvedJavaType getComponentType();

    @Override
    default ResolvedJavaType getElementalType() {
        ResolvedJavaType t = this;
        while (t.isArray()) {
            t = t.getComponentType();
        }
        return t;
    }

    @Override
    ResolvedJavaType getArrayClass();

    /**
     * Resolves the method implementation for virtual dispatches on objects of this dynamic type.
     * This resolution process only searches "up" the class hierarchy of this type. A broader search
     * that also walks "down" the hierarchy is implemented by
     * {@link #findUniqueConcreteMethod(ResolvedJavaMethod)}. For interface types it returns null
     * since no concrete object can be an interface.
     *
     * @param method the method to select the implementation of
     * @param callerType the caller or context type used to perform access checks
     * @return the link-time resolved method (might be abstract) or {@code null} if it is either a
     *         signature polymorphic method or can not be linked.
     */
    ResolvedJavaMethod resolveMethod(ResolvedJavaMethod method, ResolvedJavaType callerType);

    /**
     * A convenience wrapper for {@link #resolveMethod(ResolvedJavaMethod, ResolvedJavaType)} that
     * only returns non-abstract methods.
     *
     * @param method the method to select the implementation of
     * @param callerType the caller or context type used to perform access checks
     * @return the concrete method that would be selected at runtime, or {@code null} if there is no
     *         concrete implementation of {@code method} in this type or any of its superclasses
     */
    default ResolvedJavaMethod resolveConcreteMethod(ResolvedJavaMethod method, ResolvedJavaType callerType) {
        ResolvedJavaMethod resolvedMethod = resolveMethod(method, callerType);
        if (resolvedMethod == null || resolvedMethod.isAbstract()) {
            return null;
        }
        return resolvedMethod;
    }

    /**
     * Given a {@link ResolvedJavaMethod} A, returns a concrete {@link ResolvedJavaMethod} B that is
     * the only possible unique target for a virtual call on A(). Returns {@code null} if either no
     * such concrete method or more than one such method exists. Returns the method A if A is a
     * concrete method that is not overridden.
     * <p>
     * If the compiler uses the result of this method for its compilation, it must register an
     * assumption because dynamic class loading can invalidate the result of this method.
     *
     * @param method the method A for which a unique concrete target is searched
     * @return the unique concrete target or {@code null} if no such target exists or assumptions
     *         are not supported by this runtime
     */
    AssumptionResult<ResolvedJavaMethod> findUniqueConcreteMethod(ResolvedJavaMethod method);

    /**
     * Returns the instance fields of this class, including
     * {@linkplain ResolvedJavaField#isInternal() internal} fields. A zero-length array is returned
     * for array and primitive types. The order of fields returned by this method is stable. That
     * is, for a single JVM execution the same order is returned each time this method is called. It
     * is also the "natural" order, which means that the JVM would expect the fields in this order
     * if no specific order is given.
     *
     * @param includeSuperclasses if true, then instance fields for the complete hierarchy of this
     *            type are included in the result
     * @return an array of instance fields
     */
    ResolvedJavaField[] getInstanceFields(boolean includeSuperclasses);

    /**
     * Returns the static fields of this class, including {@linkplain ResolvedJavaField#isInternal()
     * internal} fields. A zero-length array is returned for array and primitive types. The order of
     * fields returned by this method is stable. That is, for a single JVM execution the same order
     * is returned each time this method is called.
     */
    ResolvedJavaField[] getStaticFields();

    /**
     * Returns the instance field of this class (or one of its super classes) at the given offset,
     * or {@code null} if there is no such field.
     *
     * @param offset the offset of the field to look for
     * @return the field with the given offset, or {@code null} if there is no such field.
     */
    ResolvedJavaField findInstanceFieldWithOffset(long offset, JavaKind expectedKind);

    /**
     * Returns name of source file of this type.
     */
    String getSourceFileName();

    /**
     * Returns {@code true} if the type is a local type.
     */
    boolean isLocal();

    /**
     * Returns {@code true} if the type is a member type.
     */
    boolean isMember();

    /**
     * Returns the enclosing type of this type, if it exists, or {@code null}.
     */
    ResolvedJavaType getEnclosingType();

    /**
     * Returns an array reflecting all the constructors declared by this type. This method is
     * similar to {@link Class#getDeclaredConstructors()} in terms of returned constructors. Calling
     * this method forces this type to be {@link #link linked}.
     */
    ResolvedJavaMethod[] getDeclaredConstructors();

    /**
     * Returns an array reflecting all the methods declared by this type. This method is similar to
     * {@link Class#getDeclaredMethods()} in terms of returned methods. Calling this method forces
     * this type to be {@link #link linked}.
     */
    ResolvedJavaMethod[] getDeclaredMethods();

    /**
     * Returns the {@code <clinit>} method for this class if there is one.
     */
    ResolvedJavaMethod getClassInitializer();

    default ResolvedJavaMethod findMethod(String name, Signature signature) {
        for (ResolvedJavaMethod method : getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getSignature().equals(signature)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Returns true if this type is {@link Cloneable} and can be safely cloned by creating a normal
     * Java allocation and populating it from the fields returned by
     * {@link #getInstanceFields(boolean)}. Some types may require special handling by the platform
     * so they would to go through the normal {@link Object#clone} path.
     */
    boolean isCloneableWithAllocation();

    /**
     * Lookup an unresolved type relative to an existing resolved type.
     */
    @SuppressWarnings("unused")
    default ResolvedJavaType lookupType(UnresolvedJavaType unresolvedJavaType, boolean resolve) {
        return null;
    }

    @SuppressWarnings("unused")
    default ResolvedJavaField resolveField(UnresolvedJavaField unresolvedJavaField, ResolvedJavaType accessingClass) {
        return null;
    }
}
