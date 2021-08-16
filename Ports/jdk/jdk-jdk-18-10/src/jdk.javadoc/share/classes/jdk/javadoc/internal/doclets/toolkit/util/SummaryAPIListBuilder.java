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

import java.util.*;
import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;

/**
 * Build list of all the summary packages, classes, constructors, fields and methods.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SummaryAPIListBuilder {
    /**
     * List of summary type Lists.
     */
    private final Map<SummaryElementKind, SortedSet<Element>> summaryMap;
    private final BaseConfiguration configuration;
    protected final Utils utils;
    private final Predicate<Element> belongsToSummary;

    public enum SummaryElementKind {
        MODULE,
        PACKAGE,
        INTERFACE,
        CLASS,
        ENUM,
        EXCEPTION,              // no ElementKind mapping
        ERROR,                  // no ElementKind mapping
        RECORD_CLASS,
        ANNOTATION_TYPE,
        FIELD,
        METHOD,
        CONSTRUCTOR,
        ENUM_CONSTANT,
        ANNOTATION_TYPE_MEMBER // no ElementKind mapping
    };

    /**
     * Constructor.
     *
     * @param configuration the current configuration of the doclet
     */
    public SummaryAPIListBuilder(BaseConfiguration configuration,
                                 Predicate<Element> belongsToSummary) {
        this.configuration = configuration;
        this.utils = configuration.utils;
        this.belongsToSummary = belongsToSummary;
        summaryMap = new EnumMap<>(SummaryElementKind.class);
        for (SummaryElementKind kind : SummaryElementKind.values()) {
            summaryMap.put(kind, createSummarySet());
        }
    }

    public boolean isEmpty() {
        return summaryMap.values().stream().allMatch(Set::isEmpty);
    }

    /**
     * Build the sorted list of all the summary APIs in this run.
     * Build separate lists for summary modules, packages, classes, constructors,
     * methods and fields.
     */
    protected void buildSummaryAPIInfo() {
        SortedSet<ModuleElement> modules = configuration.modules;
        SortedSet<Element> mset = summaryMap.get(SummaryElementKind.MODULE);
        for (Element me : modules) {
            if (belongsToSummary.test(me)) {
                mset.add(me);
                handleElement(me);
            }
        }
        SortedSet<PackageElement> packages = configuration.packages;
        SortedSet<Element> pset = summaryMap.get(SummaryElementKind.PACKAGE);
        for (Element pe : packages) {
            if (belongsToSummary.test(pe)) {
                pset.add(pe);
                handleElement(pe);
            }
        }
        for (Element e : configuration.getIncludedTypeElements()) {
            TypeElement te = (TypeElement)e;
            SortedSet<Element> eset;
            if (belongsToSummary.test(e)) {
                switch (e.getKind()) {
                    case ANNOTATION_TYPE -> {
                        eset = summaryMap.get(SummaryElementKind.ANNOTATION_TYPE);
                        eset.add(e);
                    }
                    case CLASS -> {
                        if (utils.isError(te)) {
                            eset = summaryMap.get(SummaryElementKind.ERROR);
                        } else if (utils.isException(te)) {
                            eset = summaryMap.get(SummaryElementKind.EXCEPTION);
                        } else {
                            eset = summaryMap.get(SummaryElementKind.CLASS);
                        }
                        eset.add(e);
                    }
                    case INTERFACE -> {
                        eset = summaryMap.get(SummaryElementKind.INTERFACE);
                        eset.add(e);
                    }
                    case ENUM -> {
                        eset = summaryMap.get(SummaryElementKind.ENUM);
                        eset.add(e);
                    }
                    case RECORD -> {
                        eset = summaryMap.get(SummaryElementKind.RECORD_CLASS);
                        eset.add(e);
                    }
                }
                handleElement(te);
            }
            composeSummaryList(summaryMap.get(SummaryElementKind.FIELD),
                    utils.getFields(te));
            composeSummaryList(summaryMap.get(SummaryElementKind.METHOD),
                    utils.getMethods(te));
            composeSummaryList(summaryMap.get(SummaryElementKind.CONSTRUCTOR),
                    utils.getConstructors(te));
            if (utils.isEnum(e)) {
                composeSummaryList(summaryMap.get(SummaryElementKind.ENUM_CONSTANT),
                        utils.getEnumConstants(te));
            }
            if (utils.isRecord(te)) {
                for (RecordComponentElement component : te.getRecordComponents()) {
                    if (belongsToSummary.test(component)) {
                        throw new AssertionError("record components not supported in summary builders: " +
                                                 "component: " + component.getSimpleName() +
                                                 " of record: " + te.getQualifiedName());
                    }
                }
            }
            if (utils.isAnnotationType(e)) {
                composeSummaryList(summaryMap.get(SummaryElementKind.ANNOTATION_TYPE_MEMBER),
                        utils.getAnnotationMembers(te));

            }
        }
    }

    /**
     * Add the members into a single list of summary members.
     *
     * @param sset set of summary elements
     * @param members members to be added in the list
     */
    private void composeSummaryList(SortedSet<Element> sset, List<? extends Element> members) {
        for (Element member : members) {
            if (belongsToSummary.test(member)) {
                sset.add(member);
                handleElement(member);
            }
        }
    }

    /**
     * Return the list of summary elements of a given type.
     *
     * @param kind the SummaryElementKind
     * @return
     */
    public SortedSet<Element> getSet(SummaryElementKind kind) {
        return summaryMap.get(kind);
    }

    /**
     * Return true if the list of a given type has size greater than 0.
     *
     * @param kind the type of list being checked.
     */
    public boolean hasDocumentation(SummaryElementKind kind) {
        return !summaryMap.get(kind).isEmpty();
    }

    /**
     * Additional extra processing of an included element.
     *
     * @param e element to process
     */
    protected void handleElement(Element e) {}

    /**
     * Create a summary set of elements.
     *
     * @return a summary set
     */
    protected final SortedSet<Element> createSummarySet() {
        return new TreeSet<>(utils.comparators.makeSummaryComparator());
    }
}
