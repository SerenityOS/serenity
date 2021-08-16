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

package jdk.javadoc.internal.doclets.toolkit.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Messages;

/**
 * Build Class Hierarchy for all the Classes. This class builds the Class
 * Tree and the Interface Tree separately.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @see java.util.HashMap
 * @see java.util.List
 */
public class ClassTree {

    /**
     * List of base classes. Used to get the mapped listing of sub-classes.
     */
    private final SortedSet<TypeElement> baseClasses;

    /**
     * Mapping for each Class with their sub classes
     */
    private final Map<TypeElement, SortedSet<TypeElement>> subClasses = new HashMap<>();

    /**
     * List of base-interfaces. Contains set of all the interfaces who do not
     * have super-interfaces. Can be used to get the mapped listing of
     * sub-interfaces.
     */
    private final SortedSet<TypeElement> baseInterfaces;

   /**
    * Mapping for each Interface with their SubInterfaces
    */
    private final Map<TypeElement, SortedSet<TypeElement>> subInterfaces = new HashMap<>();

    private final SortedSet<TypeElement> baseEnums;
    private final Map<TypeElement, SortedSet<TypeElement>> subEnums = new HashMap<>();

    private final SortedSet<TypeElement> baseAnnotationTypes;
    private final Map<TypeElement, SortedSet<TypeElement>> subAnnotationTypes = new HashMap<>();

   /**
    * Mapping for each Interface with classes who implement it.
    */
    private final Map<TypeElement, SortedSet<TypeElement>> implementingClasses = new HashMap<>();

    private final BaseConfiguration configuration;
    private final Utils utils;
    private final Comparator<Element> comparator;

    /**
     * Constructor. Build the Tree using the Root of this Javadoc run.
     *
     * @param configuration the configuration of the doclet.
     * @param noDeprecated Don't add deprecated classes in the class tree, if
     * true.
     */
    public ClassTree(BaseConfiguration configuration, boolean noDeprecated) {
        this.configuration = configuration;
        this.utils = configuration.utils;

        Messages messages = configuration.getMessages();
        messages.notice("doclet.Building_Tree");

        comparator = utils.comparators.makeClassUseComparator();
        baseAnnotationTypes = new TreeSet<>(comparator);
        baseEnums = new TreeSet<>(comparator);
        baseClasses = new TreeSet<>(comparator);
        baseInterfaces = new TreeSet<>(comparator);
        buildTree(configuration.getIncludedTypeElements());
    }

    /**
     * Constructor. Build the Tree using the Root of this Javadoc run.
     *
     * @param docEnv the DocletEnvironment.
     * @param configuration The current configuration of the doclet.
     */
    public ClassTree(DocletEnvironment docEnv, BaseConfiguration configuration) {
        this.configuration = configuration;
        this.utils = configuration.utils;
        comparator = utils.comparators.makeClassUseComparator();
        baseAnnotationTypes = new TreeSet<>(comparator);
        baseEnums = new TreeSet<>(comparator);
        baseClasses = new TreeSet<>(comparator);
        baseInterfaces = new TreeSet<>(comparator);
        buildTree(configuration.getIncludedTypeElements());
    }

    /**
     * Constructor. Build the tree for the given array of classes.
     *
     * @param classesSet a set of classes
     * @param configuration The current configuration of the doclet.
     */
    public ClassTree(SortedSet<TypeElement>classesSet, BaseConfiguration configuration) {
        this.configuration = configuration;
        this.utils = configuration.utils;
        comparator = utils.comparators.makeClassUseComparator();
        baseAnnotationTypes = new TreeSet<>(comparator);
        baseEnums = new TreeSet<>(comparator);
        baseClasses = new TreeSet<>(comparator);
        baseInterfaces = new TreeSet<>(comparator);
        buildTree(classesSet);
    }

    /**
     * Generate mapping for the sub-classes for every class in this run.
     * Return the sub-class set for java.lang.Object which will be having
     * sub-class listing for itself and also for each sub-class itself will
     * have their own sub-class lists.
     *
     * @param classes all the classes in this run.
     */
    private void buildTree(Iterable<TypeElement> classes) {
        for (TypeElement aClass : classes) {
            // In the tree page (e.g overview-tree.html) do not include
            // information of classes which are deprecated or are a part of a
            // deprecated package.
            if (configuration.getOptions().noDeprecated() &&
                    (utils.isDeprecated(aClass) ||
                    utils.isDeprecated(utils.containingPackage(aClass)))) {
                continue;
            }

            if (utils.hasHiddenTag(aClass)) {
                continue;
            }

            if (utils.isEnum(aClass)) {
                processType(aClass, configuration, baseEnums, subEnums);
            } else if (utils.isClass(aClass)) {
                processType(aClass, configuration, baseClasses, subClasses);
            } else if (utils.isInterface(aClass)) {
                processInterface(aClass);
            } else if (utils.isAnnotationType(aClass)) {
                processType(aClass, configuration, baseAnnotationTypes,
                    subAnnotationTypes);
            }
        }
    }

    /**
     * For the class passed map it to its own sub-class listing.
     * For the Class passed, get the super class,
     * if superclass is non null, (it is not "java.lang.Object")
     * get the "value" from the hashmap for this key Class
     * if entry not found create one and get that.
     * add this Class as a sub class in the set
     * Recurse till hits java.lang.Object Null SuperClass.
     *
     * @param typeElement for which sub class mapping is to be generated.
     * @param configuration the current configuration of the doclet.
     */
    private void processType(TypeElement typeElement, BaseConfiguration configuration,
            Collection<TypeElement> bases, Map<TypeElement, SortedSet<TypeElement>> subs) {
        TypeElement superclass = utils.getFirstVisibleSuperClassAsTypeElement(typeElement);
        if (superclass != null) {
            if (!add(subs, superclass, typeElement)) {
                return;
            } else {
                processType(superclass, configuration, bases, subs);
            }
        } else {     // typeElement is java.lang.Object, add it once to the set
            if (!bases.contains(typeElement)) {
                bases.add(typeElement);
            }
        }
        Set<TypeMirror> intfacs = utils.getAllInterfaces(typeElement);
        for (TypeMirror intfac : intfacs) {
            add(implementingClasses, utils.asTypeElement(intfac), typeElement);
        }
    }

    /**
     * For the interface passed get the interfaces which it extends, and then
     * put this interface in the sub-interface set of those interfaces. Do it
     * recursively. If a interface doesn't have super-interface just attach
     * that interface in the set of all the baseInterfaces.
     *
     * @param typeElement Interface under consideration.
     */
    private void processInterface(TypeElement typeElement) {
        List<? extends TypeMirror> intfacs = typeElement.getInterfaces();
        if (!intfacs.isEmpty()) {
            for (TypeMirror intfac : intfacs) {
                if (!add(subInterfaces, utils.asTypeElement(intfac), typeElement)) {
                    return;
                } else {
                    processInterface(utils.asTypeElement(intfac));   // Recurse
                }
            }
        } else {
            // we need to add all the interfaces who do not have
            // super-interfaces to baseInterfaces set to traverse them
            if (!baseInterfaces.contains(typeElement)) {
                baseInterfaces.add(typeElement);
            }
        }
    }

    /**
     * Adjust the Class Tree. Add the class interface  in to it's super classes
     * or super interface's sub-interface set.
     *
     * @param map the entire map.
     * @param superclass java.lang.Object or the super-interface.
     * @param typeElement sub-interface to be mapped.
     * @returns boolean true if class added, false if class already processed.
     */
    private boolean add(Map<TypeElement, SortedSet<TypeElement>> map, TypeElement superclass, TypeElement typeElement) {
        SortedSet<TypeElement> sset = map.computeIfAbsent(superclass, s ->  new TreeSet<>(comparator));
        if (sset.contains(typeElement)) {
            return false;
        } else {
            sset.add(typeElement);
        }
        return true;
    }

    /**
     * From the map return the set of sub-classes or sub-interfaces. If set
     * is null create a new one and return it.
     *
     * @param map The entire map.
     * @param typeElement class for which the sub-class set is requested.
     * @returns a list of sub classes.
     */
    private SortedSet<TypeElement> get(Map<TypeElement, SortedSet<TypeElement>> map, TypeElement typeElement) {
        return map.computeIfAbsent(typeElement, t ->  new TreeSet<>(comparator));
    }

    /**
     *  Return the sub-class set for the class passed.
     *
     * @param typeElement class whose sub-class set is required.
     */
    public SortedSet<TypeElement> subClasses(TypeElement typeElement) {
        return get(subClasses, typeElement);
    }

    /**
     *  Return the sub-interface set for the interface passed.
     *
     * @param typeElement interface whose sub-interface set is required.
     */
    public SortedSet<TypeElement> subInterfaces(TypeElement typeElement) {
        return get(subInterfaces, typeElement);
    }

    /**
     *  Return the set of classes which implement the interface passed.
     *
     * @param typeElement interface whose implementing-classes set is required.
     */
    public SortedSet<TypeElement> implementingClasses(TypeElement typeElement) {
        SortedSet<TypeElement> result = get(implementingClasses, typeElement);
        SortedSet<TypeElement> intfcs = allSubClasses(typeElement, false);

        // If class x implements a subinterface of typeElement, then it follows
        // that class x implements typeElement.
        Iterator<TypeElement> subInterfacesIter = intfcs.iterator();
        while (subInterfacesIter.hasNext()) {
            Iterator<TypeElement> implementingClassesIter
                    = implementingClasses(subInterfacesIter.next()).iterator();
            while (implementingClassesIter.hasNext()) {
                TypeElement c = implementingClassesIter.next();
                if (!result.contains(c)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     *  Return the sub-class/interface set for the class/interface passed.
     *
     * @param typeElement class/interface whose sub-class/interface set is required.
     * @param isEnum true if the subClasses should be forced to come from the
     * enum tree.
     */
    public SortedSet<TypeElement> directSubClasses(TypeElement typeElement, boolean isEnum) {
        return directSubClasses0(typeElement, isEnum);
    }

    private SortedSet<TypeElement> directSubClasses0(TypeElement typeElement, boolean isEnum) {
        if (isEnum) {
            return get(subEnums, typeElement);
        } else if (utils.isAnnotationType(typeElement)) {
            return get(subAnnotationTypes, typeElement);
        } else if (utils.isInterface(typeElement)) {
            return get(subInterfaces, typeElement);
        } else if (utils.isClass(typeElement)) {
            return get(subClasses, typeElement);
        } else {
            return Collections.emptySortedSet();
        }
    }

    /**
     * Return a set of all direct or indirect, sub-classes and subInterfaces
     * of the TypeElement argument.
     *
     * @param typeElement TypeElement whose sub-classes or sub-interfaces are requested.
     * @param isEnum true if the subClasses should be forced to come from the
     * enum tree.
     */
    public SortedSet<TypeElement> allSubClasses(TypeElement typeElement, boolean isEnum) {
        // new entries added to the set are searched as well, this is
        // really a work queue.
        List<TypeElement> list = new ArrayList<>(directSubClasses(typeElement, isEnum));
        for (int i = 0; i < list.size(); i++) {
            TypeElement te = list.get(i);
            SortedSet<TypeElement> tset = directSubClasses0(te, isEnum);
            for (TypeElement tte : tset) {
                if (!list.contains(tte)) {
                    list.add(tte);
                }
            }
        }
        SortedSet<TypeElement> out = new TreeSet<>(comparator);
        out.addAll(list);
        return out;
    }

    /**
     *  Return a set of base classes. This will have only one element namely
     *  the TypeElement for java.lang.Object, since this is the base class for all
     *  classes.
     */
    public SortedSet<TypeElement> baseClasses() {
        return baseClasses;
    }

    /**
     *  Return the set of base interfaces. This is the set of interfaces
     * which do not have super-interface.
     */
    public SortedSet<TypeElement> baseInterfaces() {
        return baseInterfaces;
    }

    /**
     *  Return the set of base enums. This is the set of enums
     *  which do not have super-enums.
     */
    public SortedSet<TypeElement> baseEnums() {
        return baseEnums;
    }

    /**
     * Return the set of base annotation types. This is the set
     * of annotation types which do not have super-annotation types.
     */
    public SortedSet<TypeElement> baseAnnotationTypes() {
        return baseAnnotationTypes;
    }
}
