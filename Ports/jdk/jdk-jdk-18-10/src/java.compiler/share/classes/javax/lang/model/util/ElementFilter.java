/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.lang.model.element.*;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;


/**
 * Filters for selecting just the elements of interest from a
 * collection of elements.  The returned sets and lists are new
 * collections and do use the argument as a backing store.  The
 * methods in this class do not make any attempts to guard against
 * concurrent modifications of the arguments.  The returned sets and
 * lists are mutable but unsafe for concurrent access.  A returned set
 * has the same iteration order as the argument set to a method.
 *
 * <p>If iterables and sets containing {@code null} are passed as
 * arguments to methods in this class, a {@code NullPointerException}
 * will be thrown.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @author Martin Buchholz
 * @since 1.6
 */
public class ElementFilter {
    private ElementFilter() {} // Do not instantiate.

    private static final Set<ElementKind> CONSTRUCTOR_KIND =
        Collections.unmodifiableSet(EnumSet.of(ElementKind.CONSTRUCTOR));

    private static final Set<ElementKind> FIELD_KINDS =
        Collections.unmodifiableSet(EnumSet.of(ElementKind.FIELD,
                                               ElementKind.ENUM_CONSTANT));
    private static final Set<ElementKind> METHOD_KIND =
        Collections.unmodifiableSet(EnumSet.of(ElementKind.METHOD));

    private static final Set<ElementKind> PACKAGE_KIND =
        Collections.unmodifiableSet(EnumSet.of(ElementKind.PACKAGE));

    private static final Set<ElementKind> MODULE_KIND =
        Collections.unmodifiableSet(EnumSet.of(ElementKind.MODULE));

    @SuppressWarnings("preview")
    private static final Set<ElementKind> TYPE_KINDS =
        Collections.unmodifiableSet(EnumSet.of(ElementKind.CLASS,
                                               ElementKind.ENUM,
                                               ElementKind.INTERFACE,
                                               ElementKind.RECORD,
                                               ElementKind.ANNOTATION_TYPE));

    @SuppressWarnings("preview")
    private static final Set<ElementKind> RECORD_COMPONENT_KIND =
        Set.of(ElementKind.RECORD_COMPONENT);

    /**
     * {@return a list of fields in {@code elements}}
     * @param elements the elements to filter
     */
    public static List<VariableElement>
            fieldsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, FIELD_KINDS, VariableElement.class);
    }

    /**
     * {@return a set of fields in {@code elements}}
     * @param elements the elements to filter
     */
    public static Set<VariableElement>
            fieldsIn(Set<? extends Element> elements) {
        return setFilter(elements, FIELD_KINDS, VariableElement.class);
    }

    /**
     * {@return a list of record components in {@code elements}}
     * @param elements the elements to filter
     * @since 16
     */
    public static List<RecordComponentElement>
        recordComponentsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, RECORD_COMPONENT_KIND, RecordComponentElement.class);
    }

    /**
     * {@return a set of record components in {@code elements}}
     * @param elements the elements to filter
     * @since 16
     */
    public static Set<RecordComponentElement>
    recordComponentsIn(Set<? extends Element> elements) {
        return setFilter(elements, RECORD_COMPONENT_KIND, RecordComponentElement.class);
    }

    /**
     * {@return a list of constructors in {@code elements}}
     * @param elements the elements to filter
     */
    public static List<ExecutableElement>
            constructorsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, CONSTRUCTOR_KIND, ExecutableElement.class);
    }

    /**
     * {@return a set of constructors in {@code elements}}
     * @param elements the elements to filter
     */
    public static Set<ExecutableElement>
            constructorsIn(Set<? extends Element> elements) {
        return setFilter(elements, CONSTRUCTOR_KIND, ExecutableElement.class);
    }

    /**
     * {@return a list of methods in {@code elements}}
     * @param elements the elements to filter
     */
    public static List<ExecutableElement>
            methodsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, METHOD_KIND, ExecutableElement.class);
    }

    /**
     * {@return a set of methods in {@code elements}}
     * @param elements the elements to filter
     */
    public static Set<ExecutableElement>
            methodsIn(Set<? extends Element> elements) {
        return setFilter(elements, METHOD_KIND, ExecutableElement.class);
    }

    /**
     * {@return a list of classes and interfaces in {@code elements}}
     * @param elements the elements to filter
     */
    public static List<TypeElement>
            typesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, TYPE_KINDS, TypeElement.class);
    }

    /**
     * {@return a set of types in {@code elements}}
     * @param elements the elements to filter
     */
    public static Set<TypeElement>
            typesIn(Set<? extends Element> elements) {
        return setFilter(elements, TYPE_KINDS, TypeElement.class);
    }

    /**
     * {@return a list of packages in {@code elements}}
     * @param elements the elements to filter
     */
    public static List<PackageElement>
            packagesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, PACKAGE_KIND, PackageElement.class);
    }

    /**
     * {@return a set of packages in {@code elements}}
     * @param elements the elements to filter
     */
    public static Set<PackageElement>
            packagesIn(Set<? extends Element> elements) {
        return setFilter(elements, PACKAGE_KIND, PackageElement.class);
    }

    /**
     * {@return a list of modules in {@code elements}}
     * @param elements the elements to filter
     * @since 9
     */
    public static List<ModuleElement>
            modulesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, MODULE_KIND, ModuleElement.class);
    }

    /**
     * {@return a set of modules in {@code elements}}
     * @param elements the elements to filter
     * @since 9
     */
    public static Set<ModuleElement>
            modulesIn(Set<? extends Element> elements) {
        return setFilter(elements, MODULE_KIND, ModuleElement.class);
    }

    // Assumes targetKinds and E are sensible.
    private static <E extends Element> List<E> listFilter(Iterable<? extends Element> elements,
                                                          Set<ElementKind> targetKinds,
                                                          Class<E> clazz) {
        List<E> list = new ArrayList<>();
        for (Element e : elements) {
            if (targetKinds.contains(e.getKind()))
                list.add(clazz.cast(e));
        }
        return list;
    }

    // Assumes targetKinds and E are sensible.
    private static <E extends Element> Set<E> setFilter(Set<? extends Element> elements,
                                                        Set<ElementKind> targetKinds,
                                                        Class<E> clazz) {
        // Return set preserving iteration order of input set.
        Set<E> set = new LinkedHashSet<>();
        for (Element e : elements) {
            if (targetKinds.contains(e.getKind()))
                set.add(clazz.cast(e));
        }
        return set;
    }

    /**
     * {@return a list of {@code exports} directives in {@code directives}}
     * @param directives the directives to filter
     * @since 9
     */
    public static List<ExportsDirective>
            exportsIn(Iterable<? extends Directive> directives) {
        return listFilter(directives, DirectiveKind.EXPORTS, ExportsDirective.class);
    }

    /**
     * {@return a list of {@code opens} directives in {@code directives}}
     * @param directives the directives to filter
     * @since 9
     */
    public static List<OpensDirective>
            opensIn(Iterable<? extends Directive> directives) {
        return listFilter(directives, DirectiveKind.OPENS, OpensDirective.class);
    }

    /**
     * {@return a list of {@code provides} directives in {@code directives}}
     * @param directives the directives to filter
     * @since 9
     */
    public static List<ProvidesDirective>
            providesIn(Iterable<? extends Directive> directives) {
        return listFilter(directives, DirectiveKind.PROVIDES, ProvidesDirective.class);
    }

    /**
     * {@return a list of {@code requires} directives in {@code directives}}
     * @param directives the directives to filter
     * @since 9
     */
    public static List<RequiresDirective>
            requiresIn(Iterable<? extends Directive> directives) {
        return listFilter(directives, DirectiveKind.REQUIRES, RequiresDirective.class);
    }

    /**
     * {@return a list of {@code uses} directives in {@code directives}}
     * @param directives the directives to filter
     * @since 9
     */
    public static List<UsesDirective>
            usesIn(Iterable<? extends Directive> directives) {
        return listFilter(directives, DirectiveKind.USES, UsesDirective.class);
    }

    // Assumes directiveKind and D are sensible.
    private static <D extends Directive> List<D> listFilter(Iterable<? extends Directive> directives,
                                                          DirectiveKind directiveKind,
                                                          Class<D> clazz) {
        List<D> list = new ArrayList<>();
        for (Directive d : directives) {
            if (d.getKind() == directiveKind)
                list.add(clazz.cast(d));
        }
        return list;
    }
}
