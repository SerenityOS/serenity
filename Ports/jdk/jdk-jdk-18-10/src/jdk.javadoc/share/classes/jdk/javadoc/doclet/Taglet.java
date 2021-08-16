/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.doclet;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

/**
 * The interface for a custom taglet supported by doclets such as
 * the {@link jdk.javadoc.doclet.StandardDoclet standard doclet}.
 * Custom taglets are used to handle custom tags in documentation
 * comments; custom tags can be instantiated individually as either
 * <i>block tags</i>, which appear at the end of a comment,
 * or <i>inline tags</i>, which can appear within the main body of a
 * documentation comment.
 *
 * <p> Each implementation of a taglet must provide a public no-argument constructor
 * to be used by doclets to instantiate the taglet. A doclet will interact
 * with classes implementing this interface as follows:
 *
 * <ol>
 * <li> The doclet will create an instance of a taglet using the no-arg
 *      constructor of the taglet class.
 * <li> Next, the doclet calls the {@link #init(DocletEnvironment,Doclet) init}
        method with an appropriate environment and doclet.
 * <li> Afterwards, the doclet calls {@link #getName() getName},
 *      {@link #getAllowedLocations() getAllowedLocations}, and
 *      {@link #isInlineTag() isInlineTag}, to determine the characteristics
 *      of the tags supported by the taglet.
 * <li> As appropriate, the doclet calls the
 *      {@link #toString(List,Element) toString} method on the taglet object,
 *      giving it a list of tags and the element for which the tags are part
 *      of the element's documentation comment, from which the taglet can
 *      determine the string to be included in the documentation.
 *      The doclet will typically specify any requirements on the contents of
 *      the string that is returned.
 * </ol>
 *
 * <p>If a taglet object is created and used without the above protocol being
 * followed, then the taglet's behavior is not defined by this interface
 * specification.
 *
 * @apiNote
 * It is typical for a taglet to be designed to work in conjunction with a
 * specific doclet.
 *
 * @see <a href="StandardDoclet.html#user-defined-taglets">User-Defined Taglets
 *      for the Standard Doclet</a>
 * @since 9
 */

public interface Taglet {

    /**
     * Returns the set of supported locations for block tags.
     * @return the set of supported locations for block tags
     */
    Set<Location> getAllowedLocations();

    /**
     * Indicates whether this taglet supports inline tags.
     *
     * @return true if this taglet supports inline tags
     */
    boolean isInlineTag();

    /**
     * Indicates whether this taglet supports block tags.
     *
     * @return true if this taglet supports block tags
     * @implSpec This implementation returns the inverse
     * result to {@code isInlineTag}.
     */
    default boolean isBlockTag() {
        return !isInlineTag();
    }

    /**
     * Returns the name of the tag supported by this taglet.
     * @return the name of this tag
     */
    String getName();

    /**
     * Initializes this taglet with the given doclet environment and doclet.
     *
     * @apiNote
     * The environment may be used to access utility classes for
     * {@link javax.lang.model.util.Elements elements} and
     * {@link javax.lang.model.util.Types types} if needed.
     *
     * @implSpec
     * This implementation does nothing.
     *
     * @param env the environment in which the doclet and taglet are running
     * @param doclet the doclet that instantiated this taglet
     */
    default void init(DocletEnvironment env, Doclet doclet) { }

    /**
     * Returns the string representation of a series of instances of
     * this tag to be included in the generated output.
     *
     * <p>If this taglet supports {@link #isInlineTag inline} tags, it will
     * be called once per instance of the inline tag, each time with a singleton list.
     * If this taglet supports {@link #isBlockTag block} tags, it will be called once
     * for each comment containing instances of block tags, with a list of all the instances
     * of the block tag in that comment.
     *
     * @param tags the list of instances of this tag
     * @param element the element to which the enclosing comment belongs
     * @return the string representation of the tags to be included in
     *  the generated output
     *
     * @see <a href="StandardDoclet.html#user-defined-taglets">User-Defined Taglets
     *      for the Standard Doclet</a>
     */
    String toString(List<? extends DocTree> tags, Element element);

    /**
     * The kind of location in which a tag may be used.
     */
    enum Location {
        /** In an Overview document. */
        OVERVIEW,
        /** In the documentation for a module. */
        MODULE,
        /** In the documentation for a package. */
        PACKAGE,
        /** In the documentation for a type, such as a class, interface or enum. */
        TYPE,
        /** In the documentation for a constructor. */
        CONSTRUCTOR,
        /** In the documentation for a method. */
        METHOD,
        /** In the documentation for a field. */
        FIELD
    }
}
