/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This is the common base class of all Java language record classes.
 *
 * <p>More information about records, including descriptions of the
 * implicitly declared methods synthesized by the compiler, can be
 * found in section 8.10 of
 * <cite>The Java Language Specification</cite>.
 *
 * <p>A <em>record class</em> is a shallowly immutable, transparent carrier for
 * a fixed set of values, called the <em>record components</em>.  The Java
 * language provides concise syntax for declaring record classes, whereby the
 * record components are declared in the record header.  The list of record
 * components declared in the record header form the <em>record descriptor</em>.
 *
 * <p>A record class has the following mandated members: a <em>canonical
 * constructor</em>, which must provide at least as much access as the record
 * class and whose descriptor is the same as the record descriptor;
 * a private final field corresponding to each component, whose name and
 * type are the same as that of the component; a public accessor method
 * corresponding to each component, whose name and return type are the same as
 * that of the component.  If not explicitly declared in the body of the record,
 * implicit implementations for these members are provided.
 *
 * <p>The implicit declaration of the canonical constructor has the same accessibility
 * as the record class and initializes the component fields from the corresponding
 * constructor arguments.  The implicit declaration of the accessor methods returns
 * the value of the corresponding component field.  The implicit declaration of the
 * {@link Object#equals(Object)}, {@link Object#hashCode()}, and {@link Object#toString()}
 * methods are derived from all of the component fields.
 *
 * <p>The primary reasons to provide an explicit declaration for the
 * canonical constructor or accessor methods are to validate constructor
 * arguments, perform defensive copies on mutable components, or normalize groups
 * of components (such as reducing a rational number to lowest terms.)
 *
 * <p>For all record classes, the following invariant must hold: if a record R's
 * components are {@code c1, c2, ... cn}, then if a record instance is copied
 * as follows:
 * <pre>
 *     R copy = new R(r.c1(), r.c2(), ..., r.cn());
 * </pre>
 * then it must be the case that {@code r.equals(copy)}.
 *
 * @apiNote
 * A record class that {@code implements} {@link java.io.Serializable} is said
 * to be a <i>serializable record</i>. Serializable records are serialized and
 * deserialized differently than ordinary serializable objects. During
 * deserialization the record's canonical constructor is invoked to construct
 * the record object. Certain serialization-related methods, such as readObject
 * and writeObject, are ignored for serializable records. More information about
 * serializable records can be found in the
 * <a href="{@docRoot}/../specs/serialization/serial-arch.html#serialization-of-records">
 * <cite>Java Object Serialization Specification,</cite> Section 1.13,
 * "Serialization of Records"</a>.
 *
 * @apiNote
 * A record class structure can be obtained at runtime via reflection.
 * See {@link Class#isRecord()} and {@link Class#getRecordComponents()} for more details.
 *
 * @jls 8.10 Record Types
 * @since 16
 */
public abstract class Record {
    /**
     * Constructor for record classes to call.
     */
    protected Record() {}

    /**
     * Indicates whether some other object is "equal to" this one.  In addition
     * to the general contract of {@link Object#equals(Object) Object.equals},
     * record classes must further obey the invariant that when
     * a record instance is "copied" by passing the result of the record component
     * accessor methods to the canonical constructor, as follows:
     * <pre>
     *     R copy = new R(r.c1(), r.c2(), ..., r.cn());
     * </pre>
     * then it must be the case that {@code r.equals(copy)}.
     *
     * @implSpec
     * The implicitly provided implementation returns {@code true} if
     * and only if the argument is an instance of the same record class
     * as this record, and each component of this record is equal to
     * the corresponding component of the argument; otherwise, {@code
     * false} is returned. Equality of a component {@code c} is
     * determined as follows:
     * <ul>
     *
     * <li> If the component is of a reference type, the component is
     * considered equal if and only if {@link
     * java.util.Objects#equals(Object,Object)
     * Objects.equals(this.c, r.c} would return {@code true}.
     *
     * <li> If the component is of a primitive type, using the
     * corresponding primitive wrapper class {@code PW} (the
     * corresponding wrapper class for {@code int} is {@code
     * java.lang.Integer}, and so on), the component is considered
     * equal if and only if {@code
     * PW.compare(this.c, r.c)} would return {@code 0}.
     *
     * </ul>
     *
     * Apart from the semantics described above, the precise algorithm
     * used in the implicitly provided implementation is unspecified
     * and is subject to change. The implementation may or may not use
     * calls to the particular methods listed, and may or may not
     * perform comparisons in the order of component declaration.
     *
     * @see java.util.Objects#equals(Object,Object)
     *
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if this record is equal to the
     *          argument; {@code false} otherwise.
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * Returns a hash code value for the record.
     * Obeys the general contract of {@link Object#hashCode Object.hashCode}.
     * For records, hashing behavior is constrained by the refined contract
     * of {@link Record#equals Record.equals}, so that any two records
     * created from the same components must have the same hash code.
     *
     * @implSpec
     * The implicitly provided implementation returns a hash code value derived
     * by combining appropriate hashes from each component.
     * The precise algorithm used in the implicitly provided implementation
     * is unspecified and is subject to change within the above limits.
     * The resulting integer need not remain consistent from one
     * execution of an application to another execution of the same
     * application, even if the hashes of the component values were to
     * remain consistent in this way.  Also, a component of primitive
     * type may contribute its bits to the hash code differently than
     * the {@code hashCode} of its primitive wrapper class.
     *
     * @see     Object#hashCode()
     *
     * @return  a hash code value for this record.
     */
    @Override
    public abstract int hashCode();

    /**
     * Returns a string representation of the record.
     * In accordance with the general contract of {@link Object#toString()},
     * the {@code toString} method returns a string that
     * "textually represents" this record. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * <p>
     * In addition to this general contract, record classes must further
     * participate in the invariant that any two records which are
     * {@linkplain Record#equals(Object) equal} must produce equal
     * strings.  This invariant is necessarily relaxed in the rare
     * case where corresponding equal component values might fail
     * to produce equal strings for themselves.
     *
     * @implSpec
     * The implicitly provided implementation returns a string which
     * contains the name of the record class, the names of components
     * of the record, and string representations of component values,
     * so as to fulfill the contract of this method.
     * The precise format produced by this implicitly provided implementation
     * is subject to change, so the present syntax should not be parsed
     * by applications to recover record component values.
     *
     * @see     Object#toString()
     *
     * @return  a string representation of the object.
     */
    @Override
    public abstract String toString();
}
