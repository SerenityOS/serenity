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

package jdk.javadoc.internal.doclets.formats.html;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Links;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.util.DocFile;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.IndexBuilder;
import jdk.javadoc.internal.doclets.toolkit.util.IndexItem;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * Extensions to {@code IndexBuilder} to fill in remaining fields
 * in index items: {@code containingModule}, {@code containingPackage},
 * {@code containingClass}, and {@code url}, and to write out the
 * JavaScript files.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HtmlIndexBuilder extends IndexBuilder {
    private final HtmlConfiguration configuration;

    private final Resources resources;
    private final Utils utils;
    private final HtmlIds htmlIds;

    /**
     * Creates a new {@code HtmlIndexBuilder}.
     *
     * @param configuration the current configuration of the doclet
     */
    HtmlIndexBuilder(HtmlConfiguration configuration) {
        super(configuration, configuration.getOptions().noDeprecated());
        this.configuration = configuration;
        resources = configuration.docResources;
        utils = configuration.utils;
        htmlIds = configuration.htmlIds;
    }

    /**
     * {@inheritDoc}
     *
     * After the initial work to add the element items, the remaining fields in
     * the items are also initialized.
     */
    public void addElements() {
        super.addElements();
        if (classesOnly) {
            return;
        }


        Map<String,Integer> duplicateLabelCheck = new HashMap<>();
        for (Character ch : getFirstCharacters()) {
            for (IndexItem item : getItems(ch)) {
                duplicateLabelCheck.compute(item.getFullyQualifiedLabel(utils),
                                            (k, v) -> v == null ? 1 : v + 1);
            }
        }

        for (Character ch : getFirstCharacters()) {
            for (IndexItem item : getItems(ch)) {
                if (item.isElementItem()) {
                    boolean addModuleInfo =
                            duplicateLabelCheck.get(item.getFullyQualifiedLabel(utils)) > 1;
                    addContainingInfo(item, addModuleInfo);
                }
            }
        }
    }

    private void addContainingInfo(IndexItem item, boolean addModuleInfo) {
        Element element = item.getElement();
        switch (element.getKind()) {
            case MODULE:
                break;

            case PACKAGE:
                if (configuration.showModules) {
                    item.setContainingModule(utils.getFullyQualifiedName(utils.containingModule(element)));
                }
                break;

            case CLASS:
            case ENUM:
            case RECORD:
            case ANNOTATION_TYPE:
            case INTERFACE:
                item.setContainingPackage(utils.getPackageName(utils.containingPackage(element)));
                if (configuration.showModules && addModuleInfo) {
                    item.setContainingModule(utils.getFullyQualifiedName(utils.containingModule(element)));
                }
                break;

            case CONSTRUCTOR:
            case METHOD:
            case FIELD:
            case ENUM_CONSTANT:
                TypeElement containingType = item.getContainingTypeElement();
                item.setContainingPackage(utils.getPackageName(utils.containingPackage(element)));
                item.setContainingClass(utils.getSimpleName(containingType));
                if (configuration.showModules && addModuleInfo) {
                    item.setContainingModule(utils.getFullyQualifiedName(utils.containingModule(element)));
                }
                if (utils.isExecutableElement(element)) {
                    String url = HtmlTree.encodeURL(htmlIds.forMember((ExecutableElement) element).name());
                    if (!url.equals(item.getLabel())) {
                        item.setUrl(url);
                    }
                }
                break;

            default:
                throw new Error();
        }
    }


    /**
     * Generates the set of index files used by interactive search.
     *
     * @throws DocFileIOException if there is a problem creating any of the search index files
     */
    public void createSearchIndexFiles() throws DocFileIOException {
        // add last-minute items
        if (!configuration.packages.isEmpty()) {
            IndexItem item = IndexItem.of(IndexItem.Category.PACKAGES,
                    resources.getText("doclet.All_Packages"),
                    DocPaths.ALLPACKAGES_INDEX);
            add(item);
        }
        IndexItem item = IndexItem.of(IndexItem.Category.TYPES,
                resources.getText("doclet.All_Classes_And_Interfaces"),
                DocPaths.ALLCLASSES_INDEX);
        add(item);

        for (IndexItem.Category category : IndexItem.Category.values()) {
            DocPath file;
            String varName;
            switch (category) {
                case MODULES -> {
                    file = DocPaths.MODULE_SEARCH_INDEX_JS;
                    varName = "moduleSearchIndex";
                }
                case PACKAGES -> {
                    file = DocPaths.PACKAGE_SEARCH_INDEX_JS;
                    varName = "packageSearchIndex";
                }
                case TYPES -> {
                    file = DocPaths.TYPE_SEARCH_INDEX_JS;
                    varName = "typeSearchIndex";
                }
                case MEMBERS -> {
                    file = DocPaths.MEMBER_SEARCH_INDEX_JS;
                    varName = "memberSearchIndex";
                }
                case TAGS -> {
                    file = DocPaths.TAG_SEARCH_INDEX_JS;
                    varName = "tagSearchIndex";
                }
                default -> throw new Error();
            }

            createSearchIndexFile(file, getItems(category), varName);
        }
    }

    /**
     * Creates a search index file.
     *
     * @param searchIndexJS the file for the JavaScript to be generated
     * @param indexItems    the search index items
     * @param varName       the variable name to write in the JavaScript file
     *
     * @throws DocFileIOException if there is a problem creating the search index file
     */
    private void createSearchIndexFile(DocPath searchIndexJS,
                                         SortedSet<IndexItem> indexItems,
                                         String varName)
            throws DocFileIOException
    {
        // The file needs to be created even if there are no searchIndex items
        DocFile jsFile = DocFile.createFileForOutput(configuration, searchIndexJS);
        try (Writer wr = jsFile.openWriter()) {
            wr.write(varName);
            wr.write(" = [");
            boolean first = true;
            for (IndexItem item : indexItems) {
                if (first) {
                    first = false;
                } else {
                    wr.write(",");
                }
                wr.write(item.toJSON());
            }
            wr.write("];");
            wr.write("updateSearchResults();");
        } catch (IOException ie) {
            throw new DocFileIOException(jsFile, DocFileIOException.Mode.WRITE, ie);
        }
    }
}
