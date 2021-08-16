/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.NewAPIBuilder;

import java.util.List;
import java.util.ListIterator;

import static com.sun.source.doctree.DocTree.Kind.SINCE;

/**
 * Generates a file containing a list of new API elements with the appropriate links.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 */
public class NewAPIListWriter extends SummaryListWriter<NewAPIBuilder> {

    /**
     * Constructor.
     *
     * @param configuration the configuration for this doclet
     */
    public NewAPIListWriter(NewAPIBuilder builder, HtmlConfiguration configuration, DocPath filename) {
        super(configuration, filename, PageMode.NEW, "new elements",
                Text.of(getHeading(builder, configuration)),
                "doclet.Window_New_List");
    }

    /**
     * If the "New API" page is configured this method instantiates a NewAPIListWriter
     * and generates the file.
     *
     * @param configuration the current configuration of the doclet.
     * @throws DocFileIOException if there is a problem writing the new API list
     */
    public static void generate(HtmlConfiguration configuration) throws DocFileIOException {
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.NEW)) {
            NewAPIBuilder builder = configuration.newAPIPageBuilder;
            NewAPIListWriter writer = new NewAPIListWriter(builder, configuration, DocPaths.NEW_LIST);
            writer.generateSummaryListFile(builder);
        }
    }

    @Override
    protected void addExtraSection(NewAPIBuilder list, Content content) {
        if (list.releases.size() > 1) {
            content.add(HtmlTree.SPAN(contents.getContent("doclet.New_Tabs_Intro"))
                    .addStyle(HtmlStyle.helpNote));
        }
    }

    @Override
    protected void addTableTabs(Table table, String headingKey) {
        List<String> releases = configuration.newAPIPageBuilder.releases;
        if (!releases.isEmpty()) {
            table.setDefaultTab(getTableCaption(headingKey)).setAlwaysShowDefaultTab(true);
            ListIterator<String> it = releases.listIterator(releases.size());
            while (it.hasPrevious()) {
                String release = it.previous();
                table.addTab(
                        releases.size() == 1
                                ? getTableCaption(headingKey)
                                : contents.getContent(
                                        "doclet.New_Elements_Added_In_Release", release),
                        element -> {
                            if (!utils.hasDocCommentTree(element)) {
                                return false;
                            }
                            List<? extends DocTree> since = utils.getBlockTags(element, SINCE);
                            if (since.isEmpty()) {
                                return false;
                            }
                            CommentHelper ch = utils.getCommentHelper(element);
                            return since.stream().anyMatch(tree -> release.equals(ch.getBody(tree).toString()));
                        });
            }
            getMainBodyScript().append(table.getScript());
        }
    }

    @Override
    protected void addComments(Element e, Content desc) {
        addSummaryComment(e, desc);
    }

    @Override
    protected Content getTableCaption(String headingKey) {
        return contents.getContent("doclet.New_Elements", super.getTableCaption(headingKey));
    }

    private static String getHeading(NewAPIBuilder builder, HtmlConfiguration configuration) {
        String label = configuration.getOptions().sinceLabel();
        return label == null ? configuration.docResources.getText("doclet.New_API") : label;
    }
}
