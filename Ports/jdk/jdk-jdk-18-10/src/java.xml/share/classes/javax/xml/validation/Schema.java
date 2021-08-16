/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.validation;

/**
 * Immutable in-memory representation of grammar.
 *
 * <p>
 * This object represents a set of constraints that can be checked/
 * enforced against an XML document.
 *
 * <p>
 * A {@link Schema} object is thread safe and applications are
 * encouraged to share it across many parsers in many threads.
 *
 * <p>
 * A {@link Schema} object is immutable in the sense that it shouldn't
 * change the set of constraints once it is created. In other words,
 * if an application validates the same document twice against the same
 * {@link Schema}, it must always produce the same result.
 *
 * <p>
 * A {@link Schema} object is usually created from {@link SchemaFactory}.
 *
 * <p>
 * Two kinds of validators can be created from a {@link Schema} object.
 * One is {@link Validator}, which provides highly-level validation
 * operations that cover typical use cases. The other is
 * {@link ValidatorHandler}, which works on top of SAX for better
 * modularity.
 *
 * <p>
 * This specification does not refine
 * the {@link java.lang.Object#equals(java.lang.Object)} method.
 * In other words, if you parse the same schema twice, you may
 * still get <code>!schemaA.equals(schemaB)</code>.
 *
 * @author Kohsuke Kawaguchi
 * @see <a href="http://www.w3.org/TR/xmlschema-1/">XML Schema Part 1: Structures</a>
 * @see <a href="http://www.w3.org/TR/xml11/">Extensible Markup Language (XML) 1.1</a>
 * @see <a href="http://www.w3.org/TR/REC-xml">Extensible Markup Language (XML) 1.0 (Second Edition)</a>
 * @since 1.5
 */
public abstract class Schema {

    /**
     * Constructor for the derived class.
     *
     * <p>
     * The constructor does nothing.
     */
    protected Schema() {
    }

    /**
     * Creates a new {@link Validator} for this {@link Schema}.
     *
     * <p>A validator enforces/checks the set of constraints this object
     * represents.</p>
     *
     * <p>Implementors should assure that the properties set on the
     * {@link SchemaFactory} that created this {@link Schema} are also
     * set on the {@link Validator} constructed.</p>
     *
     * @return
     *      Always return a non-null valid object.
     */
    public abstract Validator newValidator();

    /**
     * Creates a new {@link ValidatorHandler} for this {@link Schema}.
     *
     * <p>Implementors should assure that the properties set on the
     * {@link SchemaFactory} that created this {@link Schema} are also
     * set on the {@link ValidatorHandler} constructed.</p>
     *
     * @return
     *      Always return a non-null valid object.
     */
    public abstract ValidatorHandler newValidatorHandler();
}
