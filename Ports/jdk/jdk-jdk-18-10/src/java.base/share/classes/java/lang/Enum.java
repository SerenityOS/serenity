/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * This is the common base class of all Java language enumeration classes.
 *
 * More information about enums, including descriptions of the
 * implicitly declared methods synthesized by the compiler, can be
 * found in section {@jls 8.9} of <cite>The Java Language
 * Specification</cite>.
 *
 * Enumeration classes are all serializable and receive special handling
 * by the serialization mechanism. The serialized representation used
 * for enum constants cannot be customized. Declarations of methods
 * and fields that would otherwise interact with serialization are
 * ignored, including {@code serialVersionUID}; see the <cite>Java
 * Object Serialization Specification</cite> for details.
 *
 * <p> Note that when using an enumeration type as the type of a set
 * or as the type of the keys in a map, specialized and efficient
 * {@linkplain java.util.EnumSet set} and {@linkplain
 * java.util.EnumMap map} implementations are available.
 *
 * @param <E> The type of the enum subclass
 * @serial exclude
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Class#getEnumConstants()
 * @see     java.util.EnumSet
 * @see     java.util.EnumMap
 * @jls 8.9 Enum Classes
 * @jls 8.9.3 Enum Members
 * @since   1.5
 */
@SuppressWarnings("serial") // No serialVersionUID needed due to
                            // special-casing of enum classes.
public abstract class Enum<E extends Enum<E>>
        implements Constable, Comparable<E>, Serializable {
    /**
     * The name of this enum constant, as declared in the enum declaration.
     * Most programmers should use the {@link #toString} method rather than
     * accessing this field.
     */
    private final String name;

    /**
     * Returns the name of this enum constant, exactly as declared in its
     * enum declaration.
     *
     * <b>Most programmers should use the {@link #toString} method in
     * preference to this one, as the toString method may return
     * a more user-friendly name.</b>  This method is designed primarily for
     * use in specialized situations where correctness depends on getting the
     * exact name, which will not vary from release to release.
     *
     * @return the name of this enum constant
     */
    public final String name() {
        return name;
    }

    /**
     * The ordinal of this enumeration constant (its position
     * in the enum declaration, where the initial constant is assigned
     * an ordinal of zero).
     *
     * Most programmers will have no use for this field.  It is designed
     * for use by sophisticated enum-based data structures, such as
     * {@link java.util.EnumSet} and {@link java.util.EnumMap}.
     */
    private final int ordinal;

    /**
     * Returns the ordinal of this enumeration constant (its position
     * in its enum declaration, where the initial constant is assigned
     * an ordinal of zero).
     *
     * Most programmers will have no use for this method.  It is
     * designed for use by sophisticated enum-based data structures, such
     * as {@link java.util.EnumSet} and {@link java.util.EnumMap}.
     *
     * @return the ordinal of this enumeration constant
     */
    public final int ordinal() {
        return ordinal;
    }

    /**
     * Sole constructor.  Programmers cannot invoke this constructor.
     * It is for use by code emitted by the compiler in response to
     * enum class declarations.
     *
     * @param name - The name of this enum constant, which is the identifier
     *               used to declare it.
     * @param ordinal - The ordinal of this enumeration constant (its position
     *         in the enum declaration, where the initial constant is assigned
     *         an ordinal of zero).
     */
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    /**
     * Returns the name of this enum constant, as contained in the
     * declaration.  This method may be overridden, though it typically
     * isn't necessary or desirable.  An enum class should override this
     * method when a more "programmer-friendly" string form exists.
     *
     * @return the name of this enum constant
     */
    public String toString() {
        return name;
    }

    /**
     * Returns true if the specified object is equal to this
     * enum constant.
     *
     * @param other the object to be compared for equality with this object.
     * @return  true if the specified object is equal to this
     *          enum constant.
     */
    public final boolean equals(Object other) {
        return this==other;
    }

    /**
     * Returns a hash code for this enum constant.
     *
     * @return a hash code for this enum constant.
     */
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Throws CloneNotSupportedException.  This guarantees that enums
     * are never cloned, which is necessary to preserve their "singleton"
     * status.
     *
     * @return (never returns)
     */
    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Compares this enum with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * Enum constants are only comparable to other enum constants of the
     * same enum type.  The natural order implemented by this
     * method is the order in which the constants are declared.
     */
    public final int compareTo(E o) {
        Enum<?> other = (Enum<?>)o;
        Enum<E> self = this;
        if (self.getClass() != other.getClass() && // optimization
            self.getDeclaringClass() != other.getDeclaringClass())
            throw new ClassCastException();
        return self.ordinal - other.ordinal;
    }

    /**
     * Returns the Class object corresponding to this enum constant's
     * enum type.  Two enum constants e1 and  e2 are of the
     * same enum type if and only if
     *   e1.getDeclaringClass() == e2.getDeclaringClass().
     * (The value returned by this method may differ from the one returned
     * by the {@link Object#getClass} method for enum constants with
     * constant-specific class bodies.)
     *
     * @return the Class object corresponding to this enum constant's
     *     enum type
     */
    @SuppressWarnings("unchecked")
    public final Class<E> getDeclaringClass() {
        Class<?> clazz = getClass();
        Class<?> zuper = clazz.getSuperclass();
        return (zuper == Enum.class) ? (Class<E>)clazz : (Class<E>)zuper;
    }

    /**
     * Returns an enum descriptor {@code EnumDesc} for this instance, if one can be
     * constructed, or an empty {@link Optional} if one cannot be.
     *
     * @return An {@link Optional} containing the resulting nominal descriptor,
     * or an empty {@link Optional} if one cannot be constructed.
     * @since 12
     */
    @Override
    public final Optional<EnumDesc<E>> describeConstable() {
        return getDeclaringClass()
                .describeConstable()
                .map(c -> EnumDesc.of(c, name));
    }

    /**
     * Returns the enum constant of the specified enum class with the
     * specified name.  The name must match exactly an identifier used
     * to declare an enum constant in this class.  (Extraneous whitespace
     * characters are not permitted.)
     *
     * <p>Note that for a particular enum class {@code T}, the
     * implicitly declared {@code public static T valueOf(String)}
     * method on that enum may be used instead of this method to map
     * from a name to the corresponding enum constant.  All the
     * constants of an enum class can be obtained by calling the
     * implicit {@code public static T[] values()} method of that
     * class.
     *
     * @param <T> The enum class whose constant is to be returned
     * @param enumClass the {@code Class} object of the enum class from which
     *      to return a constant
     * @param name the name of the constant to return
     * @return the enum constant of the specified enum class with the
     *      specified name
     * @throws IllegalArgumentException if the specified enum class has
     *         no constant with the specified name, or the specified
     *         class object does not represent an enum class
     * @throws NullPointerException if {@code enumClass} or {@code name}
     *         is null
     * @since 1.5
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumClass,
                                                String name) {
        T result = enumClass.enumConstantDirectory().get(name);
        if (result != null)
            return result;
        if (name == null)
            throw new NullPointerException("Name is null");
        throw new IllegalArgumentException(
            "No enum constant " + enumClass.getCanonicalName() + "." + name);
    }

    /**
     * enum classes cannot have finalize methods.
     */
    @SuppressWarnings("deprecation")
    protected final void finalize() { }

    /**
     * prevent default deserialization
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException {
        throw new InvalidObjectException("can't deserialize enum");
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("can't deserialize enum");
    }

    /**
     * A <a href="{@docRoot}/java.base/java/lang/constant/package-summary.html#nominal">nominal descriptor</a> for an
     * {@code enum} constant.
     *
     * @param <E> the type of the enum constant
     *
     * @since 12
     */
    public static final class EnumDesc<E extends Enum<E>>
            extends DynamicConstantDesc<E> {

        /**
         * Constructs a nominal descriptor for the specified {@code enum} class and name.
         *
         * @param constantClass a {@link ClassDesc} describing the {@code enum} class
         * @param constantName the unqualified name of the enum constant
         * @throws NullPointerException if any argument is null
         * @jvms 4.2.2 Unqualified Names
         */
        private EnumDesc(ClassDesc constantClass, String constantName) {
            super(ConstantDescs.BSM_ENUM_CONSTANT, requireNonNull(constantName), requireNonNull(constantClass));
        }

        /**
         * Returns a nominal descriptor for the specified {@code enum} class and name
         *
         * @param <E> the type of the enum constant
         * @param enumClass a {@link ClassDesc} describing the {@code enum} class
         * @param constantName the unqualified name of the enum constant
         * @return the nominal descriptor
         * @throws NullPointerException if any argument is null
         * @jvms 4.2.2 Unqualified Names
         * @since 12
         */
        public static<E extends Enum<E>> EnumDesc<E> of(ClassDesc enumClass,
                                                        String constantName) {
            return new EnumDesc<>(enumClass, constantName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public E resolveConstantDesc(MethodHandles.Lookup lookup)
                throws ReflectiveOperationException {
            return Enum.valueOf((Class<E>) constantType().resolveConstantDesc(lookup), constantName());
        }

        @Override
        public String toString() {
            return String.format("EnumDesc[%s.%s]", constantType().displayName(), constantName());
        }
    }
}
