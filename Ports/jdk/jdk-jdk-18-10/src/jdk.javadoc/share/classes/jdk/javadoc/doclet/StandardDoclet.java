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

package jdk.javadoc.doclet;

import java.util.Locale;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.SourceVersion;

import jdk.javadoc.internal.doclets.formats.html.HtmlDoclet;

/**
 * This doclet generates HTML-formatted documentation for the specified modules,
 * packages and types.
 *
 * <h2><a id="user-defined-taglets">User-Defined Taglets</a></h2>
 *
 * The standard doclet supports user-defined {@link Taglet taglets},
 * which can be used to generate customized output for user-defined tags
 * in documentation comments.
 *
 * Taglets invoked by the standard doclet must return strings from
 * {@link Taglet#toString(List,Element) Taglet.toString} as follows:
 *
 * <dl>
 * <dt> <i>Inline Tags</i>
 * <dd> The returned string must be
 *      <a href="https://www.w3.org/TR/html52/dom.html#flow-content">flow content</a>,
 *      or any valid fragment of HTML code that may appear in the body of a document.
 *      There may be additional constraints, depending on how the tag is to be
 *      used in a documentation comment: for example, if the tag may be used
 *      within an inline element such as {@code <b>} or {@code <i>}, the taglet
 *      must not return a string containing block tags, like {@code <h3>} or
 *      {@code <p>}.
 * <dt> <i>Block Tags</i>
 * <dd> The returned string must be suitable content for a definition list,
 *      or {@code <dl>} element. It will typically be a series of pairs
 *      of {@code <dt>} and {@code <dd>} elements.
 * </dl>
 *
 * @see <a href="{@docRoot}/../specs/javadoc/doc-comment-spec.html">
 *      Documentation Comment Specification for the Standard Doclet</a>
 */
public class StandardDoclet implements Doclet {

    private final HtmlDoclet htmlDoclet;

    /**
     * Creates an instance of the standard doclet, used to generate HTML-formatted
     * documentation.
     */
    public StandardDoclet() {
        htmlDoclet = new HtmlDoclet(this);
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        htmlDoclet.init(locale, reporter);
    }

    @Override
    public String getName() {
        return "Standard";
    }

    @Override
    public Set<? extends Doclet.Option> getSupportedOptions() {
        return htmlDoclet.getSupportedOptions();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return htmlDoclet.getSupportedSourceVersion();
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        return htmlDoclet.run(docEnv);
    }

    /**
     * {@return the locale for this doclet}
     *
     * @see #init(Locale, Reporter)
     *
     * @since 17
     */
    public Locale getLocale() {
        return htmlDoclet.getConfiguration().getLocale();
    }

    /**
     * {@return the reporter for this doclet}
     *
     * @see #init(Locale, Reporter)
     *
     * @since 17
     */
    public Reporter getReporter() {
        return htmlDoclet.getConfiguration().getReporter();
    }
}
