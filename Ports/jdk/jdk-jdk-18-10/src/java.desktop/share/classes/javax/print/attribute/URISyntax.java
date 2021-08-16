/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

/**
 * Class {@code URISyntax} is an abstract base class providing the common
 * implementation of all attributes whose value is a Uniform Resource Identifier
 * (URI). Once constructed, a {@code URI} attribute's value is immutable.
 *
 * @author Alan Kaminsky
 */
public abstract class URISyntax implements Serializable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -7842661210486401678L;

    /**
     * {@code URI} value of this {@code URI} attribute.
     *
     * @serial
     */
    private URI uri;

    /**
     * Constructs a {@code URI} attribute with the specified {@code URI}.
     *
     * @param  uri the {@code URI}
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    protected URISyntax(URI uri) {
        this.uri = verify (uri);
    }

    private static URI verify(URI uri) {
        if (uri == null) {
            throw new NullPointerException(" uri is null");
        }
        return uri;
    }

    /**
     * Returns this {@code URI} attribute's {@code URI} value.
     *
     * @return the {@code URI}
     */
    public URI getURI()  {
        return uri;
    }

    /**
     * Returns a hashcode for this {@code URI} attribute.
     *
     * @return a hashcode value for this object
     */
    public int hashCode() {
        return uri.hashCode();
    }

    /**
     * Returns whether this {@code URI} attribute is equivalent to the passed in
     * object. To be equivalent, all of the following conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code URISyntax}.
     *   <li>This {@code URI} attribute's underlying {@code URI} and
     *   {@code object}'s underlying {@code URI} are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this {@code URI}
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return(object != null &&
               object instanceof URISyntax &&
               this.uri.equals (((URISyntax) object).uri));
    }

    /**
     * Returns a {@code String} identifying this {@code URI} attribute. The
     * {@code String} is the string representation of the attribute's underlying
     * {@code URI}.
     *
     * @return a {@code String} identifying this object
     */
    public String toString() {
        return uri.toString();
    }
}
