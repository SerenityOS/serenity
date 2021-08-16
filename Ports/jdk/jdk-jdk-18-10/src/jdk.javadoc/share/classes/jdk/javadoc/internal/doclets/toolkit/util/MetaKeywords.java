/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.BaseOptions;
import jdk.javadoc.internal.doclets.toolkit.Resources;

/**
 * Provides methods for creating an array of class, method and
 * field names to be included as meta keywords in the HTML header
 * of class pages.  These keywords improve search results
 * on browsers that look for keywords.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MetaKeywords {

    private final BaseOptions options;
    private final Resources resources;
    private final Utils utils;

    /**
     * Constructor
     */
    public MetaKeywords(BaseConfiguration configuration) {
        options = configuration.getOptions();
        resources = configuration.getDocResources();
        utils = configuration.utils;
    }

    /**
     * Returns an array of strings where each element
     * is a class, method or field name.  This array is
     * used to create one meta keyword tag for each element.
     * Method parameter lists are converted to "()" and
     * overloads are combined.
     *
     * Constructors are not included because they have the same
     * name as the class, which is already included.
     * Nested class members are not included because their
     * definitions are on separate pages.
     */
    public List<String> getMetaKeywords(TypeElement typeElement) {
        ArrayList<String> results = new ArrayList<>();

        // Add field and method keywords only if -keywords option is used
        if (options.keywords()) {
            results.addAll(getClassKeyword(typeElement));
            results.addAll(getMemberKeywords(utils.getFields(typeElement)));
            results.addAll(getMemberKeywords(utils.getMethods(typeElement)));
        }
        ((ArrayList)results).trimToSize();
        return results;
    }

    /**
     * Get the current class for a meta tag keyword, as the first
     * and only element of an array list.
     */
    protected List<String> getClassKeyword(TypeElement typeElement) {
        ArrayList<String> metakeywords = new ArrayList<>(1);
        String cltypelower = utils.isInterface(typeElement) ? "interface" : "class";
        metakeywords.add(utils.getFullyQualifiedName(typeElement) + " " + cltypelower);
        return metakeywords;
    }

    /**
     * Get the package keywords.
     */
    public List<String> getMetaKeywords(PackageElement packageElement) {
        List<String> result = new ArrayList<>(1);
        if (options.keywords()) {
            String pkgName = utils.getPackageName(packageElement);
            result.add(pkgName + " " + "package");
        }
        return result;
    }

    /**
     * Get the module keywords.
     *
     * @param mdle the module being documented
     */
    public List<String> getMetaKeywordsForModule(ModuleElement mdle) {
        if (options.keywords()) {
            return Arrays.asList(mdle.getQualifiedName() + " " + "module");
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get the overview keywords.
     */
    public List<String> getOverviewMetaKeywords(String title, String docTitle) {
         List<String> result = new ArrayList<>(1);
        if (options.keywords()) {
            String windowOverview = resources.getText(title);
            if (docTitle.length() > 0) {
                result.add(windowOverview + ", " + docTitle);
            } else {
                result.add(windowOverview);
            }
        }
        return result;
    }

    /**
     * Get members for meta tag keywords as an array,
     * where each member name is a string element of the array.
     * The parameter lists are not included in the keywords;
     * therefore all overloaded methods are combined.<br>
     * Example: getValue(Object) is returned in array as getValue()
     *
     * @param members  array of members to be added to keywords
     */
    protected List<String> getMemberKeywords(List<? extends Element> members) {
        ArrayList<String> results = new ArrayList<>();
        for (Element member : members) {
            String membername = utils.isMethod(member)
                    ? utils.getSimpleName(member) + "()"
                    : utils.getSimpleName(member);
            if (!results.contains(membername)) {
                results.add(membername);
            }
        }
        ((ArrayList)results).trimToSize();
        return results;
    }
}
