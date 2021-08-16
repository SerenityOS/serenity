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

package jdk.javadoc.internal.doclets.formats.html.markup;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;

/**
 * An HTML {@code <head>} element.
 *
 * Many methods return the current object, to facilitate fluent builder-style usage.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Head extends Content {
    private final Runtime.Version docletVersion;
    private final Date generatedDate;
    private final DocPath pathToRoot;
    private String title;
    private String charset;
    private final List<String> keywords;
    private String description;
    private String generator;
    private boolean showTimestamp;
    private DocPath mainStylesheet;
    private List<DocPath> additionalStylesheets = Collections.emptyList();
    private boolean index;
    private Script mainBodyScript;
    private final List<Script> scripts;
    private final List<Content> extraContent;
    private boolean addDefaultScript = true;
    private DocPath canonicalLink;

    /**
     * Creates a {@code Head} object, for a given file and HTML version.
     * The file is used to help determine the relative paths to stylesheet and script files.
     * The HTML version is used to determine the the appropriate form of a META element
     * recording the time the file was created.
     * The doclet version should also be provided for recording in the file.
     * @param path the path for the file that will include this HEAD element
     * @param docletVersion the doclet version
     */
    public Head(DocPath path, Runtime.Version docletVersion, Date generatedDate) {
        this.docletVersion = docletVersion;
        this.generatedDate = generatedDate;
        pathToRoot = path.parent().invert();
        keywords = new ArrayList<>();
        scripts = new ArrayList<>();
        extraContent = new ArrayList<>();
    }

    /**
     * Sets the title to appear in the TITLE element.
     *
     * @param title the title
     * @return this object
     */
    public Head setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the charset to be declared in a META [@code Content-TYPE} element.
     *
     * @param charset the charset
     * @return this object
     */
    // For temporary compatibility, this is currently optional.
    // Eventually, this should be a required call.
    public Head setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Sets the content for the description META element.
     */
    public Head setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the content for the generator META element.
     */
    public Head setGenerator(String generator) {
        this.generator = generator;
        return this;
    }

    /**
     * Adds a list of keywords to appear in META [@code keywords} elements.
     *
     * @param keywords the list of keywords, or null if none need to be added
     * @return this object
     */
    public Head addKeywords(List<String> keywords) {
        if (keywords != null) {
            this.keywords.addAll(keywords);
        }
        return this;
    }

    /**
     * Sets whether or not timestamps should be recorded in the HEAD element.
     * The timestamp will be recorded in a comment, and in an appropriate META
     * element, depending on the HTML version specified when this object was created.
     *
     * @param timestamp true if timestamps should be be added.
     * @return this object
     */
    // For temporary backwards compatibility, if this method is not called,
    // no 'Generated by javadoc' comment will be added.
    public Head setTimestamp(boolean timestamp) {
        showTimestamp = timestamp;
        return this;
    }

    /**
     * Sets the main and any additional stylesheets to be listed in the HEAD element.
     * The paths for the stylesheets must be relative to the root of the generated
     * documentation hierarchy.
     *
     * @param main the main stylesheet, or null to use the default
     * @param additional a list of any additional stylesheets to be included
     * @return  this object
     */
    public Head setStylesheets(DocPath main, List<DocPath> additional) {
        this.mainStylesheet = main;
        this.additionalStylesheets = additional;
        return this;
    }

    /**
     * Sets whether or not to include the supporting scripts and stylesheets for the
     * "search" feature.
     * If the feature is enabled, a {@code Script} must be provided into which some
     * JavaScript code will be injected, to be executed during page loading. The value
     * will be ignored if the feature is not enabled.
     *
     * @param index true if the supporting files are to be included
     * @param mainBodyScript the {@code Script} object, or null
     * @return this object
     */
    public Head setIndex(boolean index, Script mainBodyScript) {
        this.index = index;
        this.mainBodyScript = mainBodyScript;
        return this;
    }

    /**
     * Adds a script to be included in the HEAD element.
     *
     * @param script the script
     * @return this object
     */
    public Head addScript(Script script) {
        scripts.add(script);
        return this;
    }

    /**
     * Specifies whether or not to add a reference to a default script to be included
     * in the HEAD element.
     * The default script will normally be included; this method may be used to prevent that.
     *
     * @param addDefaultScript whether or not a default script will be included
     * @return this object
     */
    public Head addDefaultScript(boolean addDefaultScript) {
        this.addDefaultScript = addDefaultScript;
        return this;
    }

    /**
     * Specifies a value for a
     * <a href="https://en.wikipedia.org/wiki/Canonical_link_element">canonical link</a>
     * in the {@code <head>} element.
     * @param link the value for the canonical link
     */
    public void setCanonicalLink(DocPath link) {
        this.canonicalLink = link;
    }

    /**
     * Adds additional content to be included in the HEAD element.
     *
     * @param contents the content
     * @return this object
     */
    public Head addContent(Content... contents) {
        extraContent.addAll(Arrays.asList(contents));
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation always returns {@code false}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean write(Writer out, boolean atNewline) throws IOException {
        return toContent().write(out, atNewline);
    }

    /**
     * Returns the HTML for the HEAD element.
     *
     * @return the HTML
     */
    private Content toContent() {
        HtmlTree tree = new HtmlTree(TagName.HEAD);
        tree.add(getGeneratedBy(showTimestamp, generatedDate));
        tree.add(HtmlTree.TITLE(title));

        tree.add(HtmlTree.META("viewport", "width=device-width, initial-scale=1"));

        if (charset != null) { // compatibility; should this be allowed?
            tree.add(HtmlTree.META("Content-Type", "text/html", charset));
        }

        if (showTimestamp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            tree.add(HtmlTree.META("dc.created", dateFormat.format(generatedDate)));
        }

        if (description != null) {
            tree.add(HtmlTree.META("description", description));
        }

        if (generator != null) {
            tree.add(HtmlTree.META("generator", generator));
        }

        for (String k : keywords) {
            tree.add(HtmlTree.META("keywords", k));
        }

        if (canonicalLink != null) {
            HtmlTree link = new HtmlTree(TagName.LINK);
            link.put(HtmlAttr.REL, "canonical");
            link.put(HtmlAttr.HREF, canonicalLink.getPath());
            tree.add(link);
        }

        addStylesheets(tree);
        addScripts(tree);
        extraContent.forEach(tree::add);

        return tree;
    }

    private Comment getGeneratedBy(boolean timestamp, Date now) {
        String text = "Generated by javadoc"; // marker string, deliberately not localized
        text += " (" + docletVersion.feature() + ")";
        if (timestamp) {
            text += " on " + now;
        }
        return new Comment(text);
    }

    private void addStylesheets(HtmlTree tree) {
        if (mainStylesheet == null) {
            mainStylesheet = DocPaths.STYLESHEET;
        }
        addStylesheet(tree, mainStylesheet);

        for (DocPath path : additionalStylesheets) {
            addStylesheet(tree, path);
        }

        if (index) {
            // The order of the addStylesheet(...) calls is important
            addStylesheet(tree, DocPaths.JQUERY_FILES.resolve(DocPaths.JQUERY_UI_CSS));
            addStylesheet(tree, DocPaths.JQUERY_OVERRIDES_CSS);
        }
    }

    private void addStylesheet(HtmlTree tree, DocPath stylesheet) {
        tree.add(HtmlTree.LINK("stylesheet", "text/css",
                pathToRoot.resolve(stylesheet).getPath(), "Style"));
    }

    private void addScripts(HtmlTree tree) {
        if (addDefaultScript) {
            tree.add(HtmlTree.SCRIPT(pathToRoot.resolve(DocPaths.JAVASCRIPT).getPath()));
        }
        if (index) {
            if (pathToRoot != null && mainBodyScript != null) {
                String ptrPath = pathToRoot.isEmpty() ? "." : pathToRoot.getPath();
                mainBodyScript.append("var pathtoroot = ")
                        .appendStringLiteral(ptrPath + "/")
                        .append(";\n")
                        .append("loadScripts(document, 'script');");
            }
            addJQueryFile(tree, DocPaths.JQUERY_JS);
            addJQueryFile(tree, DocPaths.JQUERY_UI_JS);
        }
        for (Script script : scripts) {
            tree.add(script.asContent());
        }
    }

    private void addJQueryFile(HtmlTree tree, DocPath filePath) {
        DocPath jqueryFile = pathToRoot.resolve(DocPaths.JQUERY_FILES.resolve(filePath));
        tree.add(HtmlTree.SCRIPT(jqueryFile.getPath()));
    }
}
