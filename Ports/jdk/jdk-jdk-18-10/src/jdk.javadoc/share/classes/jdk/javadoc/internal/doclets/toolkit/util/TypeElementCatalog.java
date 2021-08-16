/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;

/**
 * This class acts as an artificial container for classes specified on the command line when
 * running Javadoc. For example, if you specify several classes from package java.lang, this class
 * will catalog those classes so that we can retrieve all of the classes from a particular package
 * later.
 *
 * <p>
 * <b>This is NOT part of any supported API. If you write code that depends on this, you do so at
 * your own risk. This code and its internal interfaces are subject to change or deletion without
 * notice.</b>
 */
public class TypeElementCatalog {

    /**
     * Stores the set of packages that the classes specified on the command line belong to. Note
     * that the default package is "".
     */
    private final SortedSet<PackageElement> packageSet;

    /**
     * Stores all classes for each package
     */
    private final Map<PackageElement, SortedSet<TypeElement>> allClasses;

    /**
     * Stores ordinary classes (excluding Exceptions and Errors) for each package
     */
    private final Map<PackageElement, SortedSet<TypeElement>> ordinaryClasses;

    /**
     * Stores exceptions for each package
     */
    private final Map<PackageElement, SortedSet<TypeElement>> exceptions;

    /**
     * Stores enums for each package.
     */
    private final Map<PackageElement, SortedSet<TypeElement>> enums;

    /**
     * Stores records for each package.
     */
    private final Map<PackageElement, SortedSet<TypeElement>> records;

    /**
     * Stores annotation types for each package.
     */
    private final Map<PackageElement, SortedSet<TypeElement>> annotationTypes;

    /**
     * Stores errors for each package
     */
    private final Map<PackageElement, SortedSet<TypeElement>> errors;

    /**
     * Stores interfaces for each package
     */
    private final Map<PackageElement, SortedSet<TypeElement>> interfaces;

    private final BaseConfiguration configuration;
    private final Utils utils;
    private final Comparator<Element> comparator;

    /**
     * Construct a new TypeElementCatalog.
     *
     * @param typeElements the array of TypeElements to catalog
     */
    public TypeElementCatalog(Iterable<TypeElement> typeElements, BaseConfiguration config) {
        this(config);
        for (TypeElement typeElement : typeElements) {
            addTypeElement(typeElement);
        }
    }

    /**
     * Construct a new TypeElementCatalog.
     *
     */
    public TypeElementCatalog(BaseConfiguration config) {
        this.configuration = config;
        this.utils = config.utils;
        comparator = utils.comparators.makeGeneralPurposeComparator();
        allClasses = new HashMap<>();
        ordinaryClasses = new HashMap<>();
        exceptions = new HashMap<>();
        enums = new HashMap<>();
        records = new HashMap<>();
        annotationTypes = new HashMap<>();
        errors = new HashMap<>();
        interfaces = new HashMap<>();
        packageSet = new TreeSet<>(comparator);
    }

    /**
     * Add the given class to the catalog.
     *
     * @param typeElement the TypeElement to add to the catalog.
     */
    public final void addTypeElement(TypeElement typeElement) {
        if (typeElement == null) {
            return;
        }
        addTypeElement(typeElement, allClasses);
        if (utils.isOrdinaryClass(typeElement)) {
            addTypeElement(typeElement, ordinaryClasses);
        } else if (utils.isException(typeElement)) {
            addTypeElement(typeElement, exceptions);
        } else if (utils.isEnum(typeElement)) {
            addTypeElement(typeElement, enums);
        } else if (utils.isRecord(typeElement)) {
            addTypeElement(typeElement, records);
        } else if (utils.isAnnotationType(typeElement)) {
            addTypeElement(typeElement, annotationTypes);
        } else if (utils.isError(typeElement)) {
            addTypeElement(typeElement, errors);
        } else if (utils.isInterface(typeElement)) {
            addTypeElement(typeElement, interfaces);
        }
    }

    /**
     * Add the given class to the given map.
     *
     * @param typeElement the class to add to the catalog.
     * @param map the Map to add the TypeElement to.
     */
    private void addTypeElement(TypeElement typeElement, Map<PackageElement, SortedSet<TypeElement>> map) {

        PackageElement pkg = utils.containingPackage(typeElement);
        if (utils.isSpecified(pkg) || configuration.getOptions().noDeprecated() && utils.isDeprecated(pkg)) {
            // No need to catalog this class if it's package is
            // specified on the command line or if -nodeprecated option is set
            return;
        }

        SortedSet<TypeElement> s = map.get(pkg);
        if (s == null) {
            packageSet.add(pkg);
            s = new TreeSet<>(comparator);
        }
        s.add(typeElement);
        map.put(pkg, s);

    }

    private SortedSet<TypeElement> getSet(Map<PackageElement, SortedSet<TypeElement>> m, PackageElement key) {
        SortedSet<TypeElement> s = m.get(key);
        if (s != null) {
            return s;
        }
        return new TreeSet<>(comparator);
    }
    /**
     * Return all of the classes specified on the command-line that belong to the given package.
     *
     * @param packageElement the package to return the classes for.
     */
    public SortedSet<TypeElement> allClasses(PackageElement packageElement) {
        return utils.isSpecified(packageElement)
                ? utils.getTypeElementsAsSortedSet(utils.getEnclosedTypeElements(packageElement))
                : getSet(allClasses, packageElement);
    }

    /**
     * Return all of the classes specified on the command-line that belong to the unnamed package.
     */
    public SortedSet<TypeElement> allUnnamedClasses() {
        for (PackageElement pkg : allClasses.keySet()) {
            if (pkg.isUnnamed()) {
                return allClasses.get(pkg);
            }
        }
        return new TreeSet<>(comparator);
    }

    /**
     * Return a SortedSet of packages that this catalog stores.
     */
    public SortedSet<PackageElement> packages() {
         return packageSet;
    }

    /**
     * Return all of the errors specified on the command-line that belong to the given package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> errors(PackageElement pkg) {
        return getSet(errors, pkg);
    }

    /**
     * Return all of the exceptions specified on the command-line that belong to the given package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> exceptions(PackageElement pkg) {
        return getSet(exceptions, pkg);
    }

    /**
     * Return all of the enums specified on the command-line that belong to the given package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> enums(PackageElement pkg) {
        return getSet(enums, pkg);
    }

    /**
     * Return all of the records specified on the command-line that belong to the given package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> records(PackageElement pkg) {
        return getSet(records, pkg);
    }

    /**
     * Return all of the annotation types specified on the command-line that belong to the given
     * package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> annotationTypes(PackageElement pkg) {
        return getSet(annotationTypes, pkg);
    }

    /**
     * Return all of the interfaces specified on the command-line that belong to the given package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> interfaces(PackageElement pkg) {
        return getSet(interfaces, pkg);
    }

    /**
     * Return all of the ordinary classes specified on the command-line that belong to the given
     * package.
     *
     * @param pkg the name of the package specified on the command-line.
     */
    public SortedSet<TypeElement> ordinaryClasses(PackageElement pkg) {
        return getSet(ordinaryClasses, pkg);
    }
}
