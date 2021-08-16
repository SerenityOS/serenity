/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Encapsulation of a link, or set of links (e.g. client side imagemap) in a
 * Hypertext document
 *
 * @author Peter Korn
 * @see Accessible
 * @see Accessible#getAccessibleContext
 * @see AccessibleContext
 * @see AccessibleText
 * @see AccessibleContext#getAccessibleText
 */
public abstract class AccessibleHyperlink implements AccessibleAction {

    /**
     * Constructor for subclasses to call.
     */
    protected AccessibleHyperlink() {}

    /**
     * Since the document a link is associated with may have changed, this
     * method returns whether or not this Link is still valid (with respect to
     * the document it references).
     *
     * @return a flag indicating whether this link is still valid with respect
     *         to the {@code AccessibleHypertext} it belongs to
     */
    public abstract boolean isValid();

    /**
     * Returns the number of accessible actions available in this Link If there
     * are more than one, the first one is NOT considered the "default" action
     * of this LINK object (e.g. in an HTML imagemap). In general, links will
     * have only one {@code AccessibleAction} in them.
     *
     * @return the zero-based number of actions in this object
     */
    public abstract int getAccessibleActionCount();

    /**
     * Performs the specified action on the object.
     *
     * @param  i zero-based index of actions
     * @return {@code true} if the action was performed; otherwise {@code false}
     * @see #getAccessibleActionCount
     */
    public abstract boolean doAccessibleAction(int i);

    /**
     * Returns a string description of this particular link action. This should
     * be a text string associated with anchoring text, this should be the
     * anchor text. E.g. from HTML: &lt;a
     * HREF="http://www.sun.com/access"&gt;Accessibility&lt;/a&gt; this method
     * would return "Accessibility".
     * <p>
     * Similarly, from this HTML: &lt;a HREF="#top"&gt;&lt;img src="top-hat.gif"
     * alt="top hat"&gt;&lt;/a&gt; this method would return "top hat"
     *
     * @param  i zero-based index of the actions
     * @return a string description of the action
     * @see #getAccessibleActionCount
     */
    public abstract String getAccessibleActionDescription(int i);

    /**
     * Returns an object that represents the link action, as appropriate for
     * that link. E.g. from HTML: &lt;a
     * HREF="http://www.sun.com/access"&gt;Accessibility&lt;/a&gt; this method
     * would return a java.net.URL("http://www.sun.com/access.html");
     *
     * @param  i zero-based index of the actions
     * @return an object representing the hypertext link itself
     * @see #getAccessibleActionCount
     */
    public abstract Object getAccessibleActionObject(int i);

    /**
     * Returns an object that represents the link anchor, as appropriate for
     * that link. E.g. from HTML: &lt;a
     * href="http://www.sun.com/access"&gt;Accessibility&lt;/a&gt; this method
     * would return a {@code String} containing the text: "Accessibility".
     * <p>
     * Similarly, from this HTML: &lt;a HREF="#top"&gt;&lt;img src="top-hat.gif"
     * alt="top hat"&gt;&lt;/a&gt; this might return the object
     * ImageIcon("top-hat.gif", "top hat");
     *
     * @param  i zero-based index of the actions
     * @return an object representing the hypertext anchor
     * @see #getAccessibleActionCount
     */
    public abstract Object getAccessibleActionAnchor(int i);

    /**
     * Gets the index with the hypertext document at which this link begins.
     *
     * @return index of start of link
     */
    public abstract int getStartIndex();

    /**
     * Gets the index with the hypertext document at which this link ends.
     *
     * @return index of end of link
     */
    public abstract int getEndIndex();
}
