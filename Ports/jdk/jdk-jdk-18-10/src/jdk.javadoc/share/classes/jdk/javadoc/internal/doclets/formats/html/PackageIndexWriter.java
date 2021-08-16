/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import javax.lang.model.element.PackageElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.Group;

/**
 * Generate the package index page "index.html".
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class PackageIndexWriter extends AbstractOverviewIndexWriter {

    /**
     * A Set of Packages to be documented.
     */
    protected SortedSet<PackageElement> packages;

    /**
     * Construct the PackageIndexWriter. Also constructs the grouping
     * information as provided on the command line by "-group" option. Stores
     * the order of groups specified by the user.
     *
     * @param configuration the configuration for this doclet
     * @param filename the path of the page to be generated
     * @see Group
     */
    public PackageIndexWriter(HtmlConfiguration configuration, DocPath filename) {
        super(configuration, filename);
        packages = configuration.packages;
    }

    /**
     * Generate the package index page.
     *
     * @param configuration the current configuration of the doclet.
     * @throws DocFileIOException if there is a problem generating the package index page
     */
    public static void generate(HtmlConfiguration configuration) throws DocFileIOException {
        DocPath filename = DocPaths.INDEX;
        PackageIndexWriter packgen = new PackageIndexWriter(configuration, filename);
        packgen.buildOverviewIndexFile("doclet.Window_Overview_Summary", "package index");
    }

    /**
     * Adds the packages list to the documentation tree.
     *
     * @param main the documentation tree to which the packages list will be added
     */
    @Override
    protected void addIndex(Content main) {
        Map<String, SortedSet<PackageElement>> groupPackageMap
                = configuration.group.groupPackages(packages);

        if (!groupPackageMap.keySet().isEmpty()) {
            Table table =  new Table(HtmlStyle.summaryTable)
                    .setHeader(getPackageTableHeader())
                    .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colLast)
                    .setId(HtmlIds.ALL_PACKAGES_TABLE)
                    .setDefaultTab(contents.getContent("doclet.All_Packages"));

            // add the tabs in command-line order
            for (String groupName : configuration.group.getGroupList()) {
                Set<PackageElement> groupPackages = groupPackageMap.get(groupName);
                if (groupPackages != null) {
                    table.addTab(Text.of(groupName), groupPackages::contains);
                }
            }

            for (PackageElement pkg : configuration.packages) {
                if (!pkg.isUnnamed()) {
                    if (!(options.noDeprecated() && utils.isDeprecated(pkg))) {
                        Content packageLinkContent = getPackageLink(pkg, getLocalizedPackageName(pkg));
                        Content summaryContent = new ContentBuilder();
                        addSummaryComment(pkg, summaryContent);
                        table.addRow(pkg, packageLinkContent, summaryContent);
                    }
                }
            }

            main.add(table);

            if (table.needsScript()) {
                getMainBodyScript().append(table.getScript());
            }
        }
    }
}
