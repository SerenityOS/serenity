/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Standard DocPath objects.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 */
public class DocPaths {
    private final String moduleSeparator;
    private final Utils utils;

    public DocPaths(Utils utils) {
        this.utils = utils;
        moduleSeparator = "/module-";
    }

    public static final DocPath DOT_DOT = DocPath.create("..");

    /** The name of the file for all classes index. */
    public static final DocPath ALLCLASSES_INDEX = DocPath.create("allclasses-index.html");

    /** The name of the file for all packages index. */
    public static final DocPath ALLPACKAGES_INDEX = DocPath.create("allpackages-index.html");

    /** The name of the sub-directory for storing class usage info. */
    public static final DocPath CLASS_USE = DocPath.create("class-use");

    /** The name of the file for constant values. */
    public static final DocPath CONSTANT_VALUES = DocPath.create("constant-values.html");

    /** The name of the file for deprecated elements. */
    public static final DocPath DEPRECATED_LIST = DocPath.create("deprecated-list.html");

    /** The name of the subdirectory for user-provided additional documentation files. */
    public static final DocPath DOC_FILES = DocPath.create("doc-files");

    /** The name of the file for the element list. */
    public static final DocPath ELEMENT_LIST = DocPath.create("element-list");

    /** The name of the image file showing a magnifying glass on the search box. */
    public static final DocPath GLASS_IMG = DocPath.create("glass.png");

    /** The name of the file for help info. */
    public static final DocPath HELP_DOC = DocPath.create("help-doc.html");

    /** The name of the main index file. */
    public static final DocPath INDEX = DocPath.create("index.html");

    /** The name of the single index file for all classes. */
    public static final DocPath INDEX_ALL = DocPath.create("index-all.html");

    /** The name of the directory for the split index files. */
    public static final DocPath INDEX_FILES = DocPath.create("index-files");

    /**
     * Generate the name of one of the files in the split index.
     * @param n the position in the index
     * @return the path
     */
    public static DocPath indexN(int n) {
        return DocPath.create("index-" + n + ".html");
    }

    /** The name of the default javascript file. */
    public static final DocPath JAVASCRIPT = DocPath.create("script.js");

    /** The name of the stylesheet file overriding jQuery UI stylesheet. */
    public static final DocPath JQUERY_OVERRIDES_CSS = DocPath.create("jquery-ui.overrides.css");

    /** The name of the directory for the jQuery files. */
    public static final DocPath JQUERY_FILES = DocPath.create("script-dir");

    /** The name of the default jQuery javascript file. */
    public static final DocPath JQUERY_JS = DocPath.create("jquery-3.5.1.min.js");

    /** The name of the default jQuery UI stylesheet file. */
    public static final DocPath JQUERY_UI_CSS = DocPath.create("jquery-ui.min.css");

    /** The name of the default jQuery UI javascript file. */
    public static final DocPath JQUERY_UI_JS = DocPath.create("jquery-ui.min.js");

    /** The name of the directory for legal files. */
    public static final DocPath LEGAL = DocPath.create("legal");

    /** The name of the member search index js file. */
    public static final DocPath MEMBER_SEARCH_INDEX_JS = DocPath.create("member-search-index.js");

    /** The name of the module search index js file. */
    public static final DocPath MODULE_SEARCH_INDEX_JS = DocPath.create("module-search-index.js");

    /** The name of the file for the overview summary. */
    public static final DocPath OVERVIEW_SUMMARY = DocPath.create("overview-summary.html");

    /** The name of the file for the overview tree. */
    public static final DocPath OVERVIEW_TREE = DocPath.create("overview-tree.html");

    /** The name of the file for the package list. This is to support the legacy mode. */
    public static final DocPath PACKAGE_LIST = DocPath.create("package-list");

    /** The name of the package search index js file. */
    public static final DocPath PACKAGE_SEARCH_INDEX_JS = DocPath.create("package-search-index.js");

    /** The name of the file for the package summary. */
    public static final DocPath PACKAGE_SUMMARY = DocPath.create("package-summary.html");

    /** The name of the file for the package tree. */
    public static final DocPath PACKAGE_TREE = DocPath.create("package-tree.html");

    /** The name of the file for the package usage info. */
    public static final DocPath PACKAGE_USE = DocPath.create("package-use.html");

    /** The name of the file for preview elements. */
    public static final DocPath PREVIEW_LIST = DocPath.create("preview-list.html");

    /** The name of the file for new elements. */
    public static final DocPath NEW_LIST = DocPath.create("new-list.html");

    /** The name of the file for all system properties. */
    public static final DocPath SYSTEM_PROPERTIES = DocPath.create("system-properties.html");

    /**
     * Returns the path for a type element.
     * For example, if the type element is {@code java.lang.Object},
     * the path is {@code java/lang/Object.html}.
     *
     * @param typeElement the type element
     * @return the path
     */
    public DocPath forClass(TypeElement typeElement) {
        return (typeElement == null)
                ? DocPath.empty
                : forPackage(utils.containingPackage(typeElement)).resolve(forName(typeElement));
    }

    /**
     * Returns the path for the simple name of a type element.
     * For example, if the type element is {@code java.lang.Object},
     * the path is {@code Object.html}.
     *
     * @param typeElement the type element
     * @return the path
     */
    public DocPath forName(TypeElement typeElement) {
        return (typeElement == null)
                ? DocPath.empty
                : new DocPath(utils.getSimpleName(typeElement) + ".html");
    }

    public static DocPath forModule(ModuleElement mdle) {
        return mdle == null || mdle.isUnnamed()
                ? DocPath.empty
                : DocPath.create(mdle.getQualifiedName().toString());
    }

    /**
     * Returns the path for the package of a type element.
     * For example, if the type element is {@code java.lang.Object},
     * the path is {@code java/lang}.
     *
     * @param typeElement the type element
     * @return the path
     */
    public DocPath forPackage(TypeElement typeElement) {
        return (typeElement == null)
                ? DocPath.empty
                : forPackage(utils.containingPackage(typeElement));
    }

    /**
     * Returns the path for a package.
     * For example, if the package is {@code java.lang},
     * the path is {@code java/lang}.
     *
     * @param pkgElement the package element
     * @return the path
     */
    public DocPath forPackage(PackageElement pkgElement) {
        if (pkgElement == null || pkgElement.isUnnamed()) {
            return DocPath.empty;
        }

        DocPath pkgPath = DocPath.create(pkgElement.getQualifiedName().toString().replace('.', '/'));
        ModuleElement mdle = (ModuleElement) pkgElement.getEnclosingElement();
        return forModule(mdle).resolve(pkgPath);
    }

    /**
     * Returns the inverse path for a package.
     * For example, if the package is {@code java.lang},
     * the inverse path is {@code ../..}.
     *
     * @param pkgElement the package element
     * @return the path
     */
    public static DocPath forRoot(PackageElement pkgElement) {
        String name = (pkgElement == null || pkgElement.isUnnamed())
                ? ""
                : pkgElement.getQualifiedName().toString();
        return new DocPath(name.replace('.', '/').replaceAll("[^/]+", ".."));
    }

    /**
     * Returns a relative path from one package to another.
     *
     * @param from the origin of the relative path
     * @param to the destination of the relative path
     * @return the path
     */
    public DocPath relativePath(PackageElement from, PackageElement to) {
        return forRoot(from).resolve(forPackage(to));
    }

    /**
     * The path for the output directory for module documentation files.
     * @param mdle the module
     * @return the path
     */
    public DocPath moduleDocFiles(ModuleElement mdle) {
        return createModulePath(mdle, "doc-files");
    }

    /**
     * The path for the file for a module's summary page.
     * @param mdle the module
     * @return the path
     */
    public DocPath moduleSummary(ModuleElement mdle) {
        return createModulePath(mdle, "summary.html");
    }

    /**
     * The path for the file for a module's summary page.
     * @param mdleName the module
     * @return the path
     */
    public DocPath moduleSummary(String mdleName) {
        return createModulePath(mdleName, "summary.html");
    }

    private DocPath createModulePath(ModuleElement mdle, String path) {
        return DocPath.create(mdle.getQualifiedName() + moduleSeparator + path);
    }

    private DocPath createModulePath(String moduleName, String path) {
        return DocPath.create(moduleName + moduleSeparator + path);
    }

    /** The name of the sub-package from which resources are read. */
    public static final DocPath RESOURCES = DocPath.create("resources");

    /** The name of the search javascript file. */
    public static final DocPath SEARCH_JS = DocPath.create("search.js");

    /** The name of the template for the search javascript file. */
    public static final DocPath SEARCH_JS_TEMPLATE = DocPath.create("search.js.template");

    /** The name of the file for the serialized form info. */
    public static final DocPath SERIALIZED_FORM = DocPath.create("serialized-form.html");

    /** The name of the directory in which HTML versions of the source code
     *  are generated.
     */
    public static final DocPath SOURCE_OUTPUT = DocPath.create("src-html");

    /** The name of the default stylesheet. */
    public static final DocPath STYLESHEET = DocPath.create("stylesheet.css");

    /** The name of the tag search index js file. */
    public static final DocPath TAG_SEARCH_INDEX_JS = DocPath.create("tag-search-index.js");

    /** The name of the type search index js file. */
    public static final DocPath TYPE_SEARCH_INDEX_JS = DocPath.create("type-search-index.js");

    /** The name of the image file for undo button on the search box. */
    public static final DocPath X_IMG = DocPath.create("x.png");

}
