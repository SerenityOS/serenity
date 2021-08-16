/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.taglet;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import jdk.javadoc.doclet.Taglet;

import static com.sun.source.doctree.DocTree.Kind.*;

/**
 * A base class for block tags to insert a link to an external copy of JLS or JVMS.
 * The tags can be used as follows:
 *
 * <pre>
 * &commat;jls section-number description
 * </pre>
 *
 * For example:
 *
 * <pre>
 * &commat;jls 3.4 Line Terminators
 * </pre>
 *
 * will produce the following HTML for a docs build configured for Java SE 12.
 *
 * <pre>{@code
 * <dt>See <i>Java Language Specification</i>:
 * <dd><a href="https://docs.oracle.com/javase/specs/jls/se12/html/jls-3.html#jls-3.4">3.4 Line terminators</a>
 * }</pre>
 *
 * The version of the spec must be set in the jspec.version system property.
 */
public class JSpec implements Taglet  {
    static final String SPEC_VERSION;

    static {
        SPEC_VERSION = System.getProperty("jspec.version");
        if (SPEC_VERSION == null) {
            throw new RuntimeException("jspec.version property not set");
        }
    }

    public static class JLS extends JSpec {
        public JLS() {
            super("jls",
                "Java Language Specification",
                "https://docs.oracle.com/javase/specs/jls/se" + SPEC_VERSION + "/html",
                "jls");
        }
    }

    public static class JVMS extends JSpec {
        public JVMS() {
            super("jvms",
                "Java Virtual Machine Specification",
                "https://docs.oracle.com/javase/specs/jvms/se" + SPEC_VERSION + "/html",
                "jvms");
        }
    }

    private String tagName;
    private String specTitle;
    private String baseURL;
    private String idPrefix;

    JSpec(String tagName, String specTitle, String baseURL, String idPrefix) {
        this.tagName = tagName;
        this.specTitle = specTitle;
        this.baseURL = baseURL;
        this.idPrefix = idPrefix;
    }

    private static final Pattern TAG_PATTERN = Pattern.compile("(?s)(.+ )?(?<chapter>[1-9][0-9]*)(?<section>[0-9.]*)( .*)?$");

    /**
     * Returns the set of locations in which the tag may be used.
     */
    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.allOf(jdk.javadoc.doclet.Taglet.Location.class);
    }

    @Override
    public boolean isBlockTag() {
        return true;
    }

    @Override
    public boolean isInlineTag() {
        return true;
    }

    @Override
    public String getName() {
        return tagName;
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element elem) {

        if (tags.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        boolean in_dd = false;

        for (DocTree tag : tags) {
            if (sb.length() == 0 && tag.getKind() == DocTree.Kind.UNKNOWN_BLOCK_TAG) {
                sb.append("<dt>See <i>").append(specTitle).append("</i>:</dt>\n")
                        .append("<dd>\n");
                in_dd = true;
            }

            List<? extends DocTree> contents;
            switch (tag.getKind()) {
                case UNKNOWN_BLOCK_TAG:
                    contents = ((UnknownBlockTagTree) tag).getContent();
                    break;
                case UNKNOWN_INLINE_TAG:
                    contents = ((UnknownInlineTagTree) tag).getContent();
                    break;
                default:
                    continue;
            }

            String tagText = contents.toString().trim();
            Matcher m = TAG_PATTERN.matcher(tagText);
            if (m.find()) {
                String chapter = m.group("chapter");
                String section = m.group("section");

                String url = String.format("%1$s/%2$s-%3$s.html#%2$s-%3$s%4$s",
                        baseURL, idPrefix, chapter, section);

                sb.append("<a href=\"")
                        .append(url)
                        .append("\">")
                        .append(expand(contents))
                        .append("</a>");

                if (tag.getKind() == DocTree.Kind.UNKNOWN_BLOCK_TAG) {
                    sb.append("<br>");
                }
            }
        }

        if (in_dd) {
            sb.append("</dd>");
        }

        return sb.toString();
    }


    private String expand(List<? extends DocTree> trees) {
        return (new SimpleDocTreeVisitor<StringBuilder, StringBuilder>() {
            public StringBuilder defaultAction(DocTree tree, StringBuilder sb) {
                return sb.append(tree.toString());
            }

            public StringBuilder visitLiteral(LiteralTree tree, StringBuilder sb) {
                if (tree.getKind() == CODE) {
                    sb.append("<code>");
                }
                sb.append(escape(tree.getBody().toString()));
                if (tree.getKind() == CODE) {
                    sb.append("</code>");
                }
                return sb;
            }

            private String escape(String s) {
                return s.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
            }
        }).visit(trees, new StringBuilder()).toString();
    }
}
