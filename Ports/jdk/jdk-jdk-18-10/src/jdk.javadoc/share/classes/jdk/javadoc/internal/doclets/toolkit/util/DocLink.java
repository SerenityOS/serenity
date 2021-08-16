/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

/**
 * Abstraction for simple relative URIs, consisting of a path and an
 * optional fragment. DocLink objects can be created by the constructors
 * below or from a DocPath using the convenience
 * {@link DocPath#fragment fragment} method.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 */
public class DocLink {
    final DocPath path;
    final String fragment;

    /**
     * Creates a DocLink representing the URI {@code #fragment}.
     * @param fragment the fragment
     * @return the DocLink
     */
    public static DocLink fragment(String fragment) {
        return new DocLink((DocPath) null, fragment);
    }

    /**
     * Creates a DocLink representing the URI {@code path}.
     * @param path the path
     */
    public DocLink(DocPath path) {
        this(path, null);
    }

    /**
     * Creates a DocLink representing the URI {@code path#fragment}.
     * Any of the component parts may be null.
     * @param path the path
     * @param fragment the fragment
     */
    public DocLink(DocPath path, String fragment) {
        this.path = path;
        this.fragment = fragment;
    }

    /**
     * Creates a DocLink representing the URI {@code path#fragment}.
     * Any of the component parts may be null.
     * @param path the path
     * @param fragment the fragment
     */
    public DocLink(String path, String fragment) {
        this(DocPath.create(path), fragment);
    }

    /**
     * Creates a DocLink formed by relativizing the path against a given base.
     * @param base the base
     * @return the DocLink
     */
    public DocLink relativizeAgainst(DocPath base) {
        if (base.isEmpty() || path == null) {
            return this;
        }

        // The following guards against the (ugly) use-case of using DocPath to contain a URL
        if (isAbsoluteURL(path)) {
            return this;
        }

        DocPath newPath = base.relativize(path);
        // avoid generating an empty link by using the basename of the path if necessary
        if (newPath.isEmpty() && isEmpty(fragment)) {
            newPath = path.basename();
        }
        return new DocLink(newPath, fragment);
    }

    public DocLink withFragment(String fragment) {
        return new DocLink(path, fragment);
    }

    // return true if the path begins <letters>://
    private boolean isAbsoluteURL(DocPath path) {
        String s = path.getPath();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                continue;
            }
            return (c == ':' && i + 2 < s.length() && s.charAt(i + 1)== '/' && s.charAt(i + 2)== '/');
        }
        return false;
    }

    /**
     * Returns the link in the form "path#fragment", omitting any empty
     * components.
     * @return the string
     */
    @Override
    public String toString() {
        // common fast path
        if (path != null && isEmpty(fragment))
            return path.getPath();

        StringBuilder sb = new StringBuilder();
        if (path != null)
            sb.append(path.getPath());
        if (!isEmpty(fragment))
            sb.append("#").append(fragment);
        return sb.toString();
    }

    private static boolean isEmpty(String s) {
        return (s == null) || s.isEmpty();
    }
}
