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

package javax.accessibility;

/**
 * The {@code AccessibleHypertext} class is the base class for all classes that
 * present hypertext information on the display. This class provides the
 * standard mechanism for an assistive technology to access that text via its
 * content, attributes, and spatial location. It also provides standard
 * mechanisms for manipulating hyperlinks. Applications can determine if an
 * object supports the {@code AccessibleHypertext} interface by first obtaining
 * its {@code AccessibleContext} (see {@link Accessible}) and then calling the
 * {@link AccessibleContext#getAccessibleText} method of
 * {@code AccessibleContext}. If the return value is a class which extends
 * {@code AccessibleHypertext}, then that object supports
 * {@code AccessibleHypertext}.
 *
 * @author Peter Korn
 * @see Accessible
 * @see Accessible#getAccessibleContext
 * @see AccessibleContext
 * @see AccessibleText
 * @see AccessibleContext#getAccessibleText
 */
public interface AccessibleHypertext extends AccessibleText {

    /**
     * Returns the number of links within this hypertext document.
     *
     * @return number of links in this hypertext doc
     */
    public abstract int getLinkCount();

    /**
     * Returns the nth Link of this Hypertext document.
     *
     * @param  linkIndex within the links of this Hypertext
     * @return Link object encapsulating the nth link(s)
     */
    public abstract AccessibleHyperlink getLink(int linkIndex);

    /**
     * Returns the index into an array of hyperlinks that is associated with
     * this character index, or -1 if there is no hyperlink associated with this
     * index.
     *
     * @param  charIndex index within the text
     * @return index into the set of hyperlinks for this hypertext doc
     */
    public abstract int getLinkIndex(int charIndex);
}
