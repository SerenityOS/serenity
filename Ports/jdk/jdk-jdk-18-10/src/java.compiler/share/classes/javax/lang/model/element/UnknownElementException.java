/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.element;

import javax.lang.model.UnknownEntityException;

/**
 * Indicates that an unknown kind of element was encountered.  This
 * can occur if the language evolves and new kinds of elements are
 * added to the {@code Element} hierarchy.  May be thrown by an
 * {@linkplain ElementVisitor element visitor} to indicate that the
 * visitor was created for a prior version of the language.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see ElementVisitor#visitUnknown
 * @since 1.6
 */
public class UnknownElementException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private transient Element element;
    private transient Object parameter;

    /**
     * Creates a new {@code UnknownElementException}.  The {@code p}
     * parameter may be used to pass in an additional argument with
     * information about the context in which the unknown element was
     * encountered; for example, the visit methods of {@link
     * ElementVisitor} may pass in their additional parameter.
     *
     * @param e the unknown element, may be {@code null}
     * @param p an additional parameter, may be {@code null}
     */
    public UnknownElementException(Element e, Object p) {
        super("Unknown element: \"" + e + "\"");
        element = e;
        this.parameter = p;
    }

    /**
     * Returns the unknown element.
     * The value may be unavailable if this exception has been
     * serialized and then read back in.
     *
     * @return the unknown element, or {@code null} if unavailable
     */
    public Element getUnknownElement() {
        return element;
    }

    /**
     * Returns the additional argument.
     *
     * @return the additional argument, or {@code null} if unavailable
     */
    public Object getArgument() {
        return parameter;
    }
}
