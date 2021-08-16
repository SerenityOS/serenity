/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: URIReference.java,v 1.4 2005/05/10 15:47:42 mullan Exp $
 */
package javax.xml.crypto;

/**
 * Identifies a data object via a URI-Reference, as specified by
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
 *
 * <p>Note that some subclasses may not have a <code>type</code> attribute
 * and for objects of those types, the {@link #getType} method always returns
 * <code>null</code>.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see URIDereferencer
 */
public interface URIReference {

    /**
     * Returns the URI of the referenced data object.
     *
     * @return the URI of the data object in RFC 2396 format (may be
     *    <code>null</code> if not specified)
     */
    String getURI();

    /**
     * Returns the type of data referenced by this URI.
     *
     * @return the type (a URI) of the data object (may be <code>null</code>
     *    if not specified)
     */
    String getType();
}
