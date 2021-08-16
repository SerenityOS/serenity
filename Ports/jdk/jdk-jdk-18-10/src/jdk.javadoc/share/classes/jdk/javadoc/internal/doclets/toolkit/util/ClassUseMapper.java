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

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor9;
import javax.lang.model.util.Types;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Map all class uses for a given class.
 *
 * <p>
 * <b>This is NOT part of any supported API. If you write code that depends on this, you do so at
 * your own risk. This code and its internal interfaces are subject to change or deletion without
 * notice.</b>
 */
public class ClassUseMapper {

    private final ClassTree classtree;

    /**
     * Mapping of TypeElements to set of PackageElements used by that class.
     */
    public final Map<TypeElement, Set<PackageElement>> classToPackage;

    /**
     * Mapping of TypeElements representing annotations to a set of PackageElements that use the annotation.
     */
    public final Map<TypeElement, List<PackageElement>> classToPackageAnnotations = new HashMap<>();

    /**
     * Mapping of TypeElements to a set of TypeElements used by that class.
     */
    public final Map<TypeElement, Set<TypeElement>> classToClass = new HashMap<>();

    /**
     * Mapping of TypeElements to a list of TypeElements which are direct or indirect subClasses of
     * that class.
     */
    public final Map<TypeElement, List<TypeElement>> classToSubclass = new HashMap<>();

    /**
     * Mapping of TypeElements to list of TypeElements which are direct or indirect subInterfaces of
     * that interface.
     */
    public final Map<TypeElement, List<TypeElement>> classToSubinterface = new HashMap<>();

    /**
     * Mapping of TypeElements to list of TypeElements which implement this interface.
     */
    public Map<TypeElement, List<TypeElement>> classToImplementingClass = new HashMap<>();

    /**
     * Mapping of TypeElements to list of VariableElements declared as that class.
     */
    public final Map<TypeElement, List<VariableElement>> classToField = new HashMap<>();

    /**
     * Mapping of TypeElements to list of ExecutableElements returning that class.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodReturn = new HashMap<>();

    /**
     * Mapping of TypeElements to list of ExecutableElements having that class as an arg.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodArgs = new HashMap<>();

    /**
     * Mapping of TypeElements to list of ExecutableElements which throws that class.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodThrows = new HashMap<>();

    /**
     * Mapping of TypeElements to list of ExecutableElements (constructors) having that
     * class as an arg.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToConstructorArgs = new HashMap<>();

    /**
     * Mapping of TypeElements to list of constructors which throws that class.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToConstructorThrows = new HashMap<>();

    /**
     * The mapping of TypeElements representing annotations to constructors that use them.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToConstructorAnnotations = new HashMap<>();

    /**
     * The mapping of TypeElement representing annotations to constructor parameters that use them.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToConstructorParamAnnotation = new HashMap<>();

    /**
     * The mapping of TypeElements to constructor arguments that use them as type parameters.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToConstructorArgTypeParam = new HashMap<>();

    /**
     * The mapping of TypeElement to TypeElement that use them as type parameters.
     */
    public final Map<TypeElement, List<TypeElement>> classToClassTypeParam = new HashMap<>();

    /**
     * The mapping of TypeElement representing annotation to TypeElements that use them.
     */
    public final Map<TypeElement, List<TypeElement>> classToClassAnnotations = new HashMap<>();

    /**
     * The mapping of TypeElement to methods that use them as type parameters.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodTypeParam = new HashMap<>();

    /**
     * The mapping of TypeElement to method arguments that use them as type parameters.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodArgTypeParam = new HashMap<>();

    /**
     * The mapping of TypeElement representing annotation to methods that use them.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodAnnotations = new HashMap<>();

    /**
     * The mapping of TypeElements to methods that have return type with type parameters
     * of that class.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodReturnTypeParam = new HashMap<>();

    /**
     * The mapping of TypeElements representing annotations to method parameters that use them.
     */
    public final Map<TypeElement, List<ExecutableElement>> classToMethodParamAnnotation = new HashMap<>();

    /**
     * The mapping of TypeElements to fields that use them as type parameters.
     */
    public final Map<TypeElement, List<VariableElement>> classToFieldTypeParam = new HashMap<>();

    /**
     * The mapping of TypeElements representing annotation to fields that use them.
     */
    public final Map<TypeElement, List<VariableElement>> annotationToField = new HashMap<>();

    private final DocletEnvironment docEnv;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final Utils utils;
    private final Comparators comparators;

    public ClassUseMapper(BaseConfiguration configuration, ClassTree classtree) {
        docEnv = configuration.docEnv;
        elementUtils = docEnv.getElementUtils();
        typeUtils = docEnv.getTypeUtils();
        utils = configuration.utils;
        comparators = utils.comparators;
        this.classtree = classtree;
        classToPackage = new TreeMap<>(comparators.makeClassUseComparator());
        // Map subclassing, subinterfacing implementing, ...
        for (TypeElement te : classtree.baseClasses()) {
            subclasses(te);
        }
        for (TypeElement intfc : classtree.baseInterfaces()) {
            // does subinterfacing as side-effect
            implementingClasses(intfc);
        }
        // Map methods, fields, constructors using a class.
        Set<TypeElement> classes = configuration.getIncludedTypeElements();
        for (TypeElement aClass : classes) {
            PackageElement pkg = elementUtils.getPackageOf(aClass);
            mapAnnotations(classToPackageAnnotations, pkg, pkg);
            mapTypeParameters(classToClassTypeParam, aClass, aClass);
            mapAnnotations(classToClassAnnotations, aClass, aClass);
            VisibleMemberTable vmt = configuration.getVisibleMemberTable(aClass);

            List<VariableElement> fields = ElementFilter.fieldsIn(vmt.getVisibleMembers(FIELDS));
            for (VariableElement fd : fields) {
                mapTypeParameters(classToFieldTypeParam, fd, fd);
                mapAnnotations(annotationToField, fd, fd);
                SimpleTypeVisitor9<Void, VariableElement> stv = new SimpleTypeVisitor9<Void, VariableElement>() {
                    @Override
                    public Void visitArray(ArrayType t, VariableElement p) {
                        return visit(t.getComponentType(), p);
                    }

                    @Override
                    public Void visitDeclared(DeclaredType t, VariableElement p) {
                        add(classToField, (TypeElement) t.asElement(), p);
                        return null;
                    }
                    @Override
                    public Void visitTypeVariable(TypeVariable t, VariableElement p) {
                        return visit(typeUtils.erasure(t), p);
                    }
                };
                stv.visit(fd.asType(), fd);
            }

            List<ExecutableElement> ctors = ElementFilter.constructorsIn(vmt.getMembers(CONSTRUCTORS));
            for (ExecutableElement ctor : ctors) {
                mapAnnotations(classToConstructorAnnotations, ctor, ctor);
                mapExecutable(ctor);
            }

            List<ExecutableElement> methods = ElementFilter.methodsIn(vmt.getMembers(METHODS));

            for (ExecutableElement method : methods) {
                mapExecutable(method);
                mapTypeParameters(classToMethodTypeParam, method, method);
                mapAnnotations(classToMethodAnnotations, method, method);
                SimpleTypeVisitor9<Void, ExecutableElement> stv = new SimpleTypeVisitor9<Void, ExecutableElement>() {
                    @Override
                    public Void visitArray(ArrayType t, ExecutableElement p) {
                        TypeMirror componentType = t.getComponentType();
                        return visit(utils.isTypeVariable(componentType)
                                ? typeUtils.erasure(componentType)
                                : componentType, p);
                    }

                    @Override
                    public Void visitDeclared(DeclaredType t, ExecutableElement p) {
                        mapTypeParameters(classToMethodReturnTypeParam, t, p);
                        add(classToMethodReturn, (TypeElement) t.asElement(), p);
                        return null;
                    }

                    @Override
                    protected Void defaultAction(TypeMirror e, ExecutableElement p) {
                        return null;
                    }
                };
                stv.visit(method.getReturnType(), method);
            }
        }
    }

    /**
     * Return all subClasses of a class AND fill-in classToSubclass map.
     */
    private Collection<TypeElement> subclasses(TypeElement te) {
        Collection<TypeElement> ret = classToSubclass.get(te);
        if (ret == null) {
            ret = new TreeSet<>(comparators.makeClassUseComparator());
            Set<TypeElement> subs = classtree.subClasses(te);
            if (subs != null) {
                ret.addAll(subs);
                for (TypeElement sub : subs) {
                    ret.addAll(subclasses(sub));
                }
            }
            addAll(classToSubclass, te, ret);
        }
        return ret;
    }

    /**
     * Return all subInterfaces of an interface AND fill-in classToSubinterface map.
     */
    private Collection<TypeElement> subinterfaces(TypeElement te) {
        Collection<TypeElement> ret = classToSubinterface.get(te);
        if (ret == null) {
            ret = new TreeSet<>(comparators.makeClassUseComparator());
            Set<TypeElement> subs = classtree.subInterfaces(te);
            if (subs != null) {
                ret.addAll(subs);
                for (TypeElement sub : subs) {
                    ret.addAll(subinterfaces(sub));
                }
            }
            addAll(classToSubinterface, te, ret);
        }
        return ret;
    }

    /**
     * Return all implementing classes of an interface (including all subClasses of implementing
     * classes and all classes implementing subInterfaces) AND fill-in both classToImplementingClass
     * and classToSubinterface maps.
     */
    private Collection<TypeElement> implementingClasses(TypeElement te) {
        Collection<TypeElement> ret = classToImplementingClass.get(te);
        if (ret == null) {
            ret = new TreeSet<>(comparators.makeClassUseComparator());
            Set<TypeElement> impl = classtree.implementingClasses(te);
            if (impl != null) {
                ret.addAll(impl);
                for (TypeElement anImpl : impl) {
                    ret.addAll(subclasses(anImpl));
                }
            }
            for (TypeElement intfc : subinterfaces(te)) {
                ret.addAll(implementingClasses(intfc));
            }
            addAll(classToImplementingClass, te, ret);
        }
        return ret;
    }

    /**
     * Determine classes used by a method or constructor, so they can be inverse mapped.
     */
    private void mapExecutable(ExecutableElement ee) {
        final boolean isConstructor = utils.isConstructor(ee);
        Set<TypeMirror> classArgs = new TreeSet<>(comparators.makeTypeMirrorClassUseComparator());
        for (VariableElement param : ee.getParameters()) {
            TypeMirror pType = param.asType();
            // primitives don't get mapped and type variables are mapped elsewhere
            if (!pType.getKind().isPrimitive() && !utils.isTypeVariable(pType)) {
                // no duplicates please
                if (classArgs.add(pType)) {
                    new SimpleTypeVisitor9<Void, ExecutableElement>() {
                        @Override
                        public Void visitArray(ArrayType t, ExecutableElement p) {
                            return visit(t.getComponentType(), p);
                        }

                        @Override
                        public Void visitDeclared(DeclaredType t, ExecutableElement p) {
                            add(isConstructor
                                    ? classToConstructorArgs
                                    : classToMethodArgs,
                                    (TypeElement) t.asElement(), p);
                            return null;
                        }
                        @Override
                        public Void visitTypeVariable(TypeVariable t, ExecutableElement p) {
                            visit(typeUtils.erasure(t), p);
                            return null;
                        }
                    }.visit(pType, ee);
                    mapTypeParameters(isConstructor
                            ? classToConstructorArgTypeParam
                            : classToMethodArgTypeParam,
                            pType, ee);
                }
            }
            mapAnnotations(isConstructor
                    ? classToConstructorParamAnnotation
                    : classToMethodParamAnnotation,
                    param, ee);

        }
        for (TypeMirror anException : ee.getThrownTypes()) {
            SimpleTypeVisitor9<Void, ExecutableElement> stv = new SimpleTypeVisitor9<Void, ExecutableElement>() {

                @Override
                public Void visitArray(ArrayType t, ExecutableElement p) {
                    super.visit(t.getComponentType(), p);
                    return null;
                }

                @Override
                public Void visitDeclared(DeclaredType t, ExecutableElement p) {
                    add(isConstructor ? classToConstructorThrows : classToMethodThrows,
                            (TypeElement) t.asElement(), p);
                    return null;
                }

                @Override
                public Void visitError(ErrorType t, ExecutableElement p) {
                    add(isConstructor ? classToConstructorThrows : classToMethodThrows,
                            (TypeElement) t.asElement(), p);
                    return null;
                }

                @Override
                protected Void defaultAction(TypeMirror e, ExecutableElement p) {
                    throw new AssertionError("this should not happen");
                }
            };

            stv.visit(typeUtils.erasure(anException), ee);
        }
    }

    private <T> List<T> refList(Map<TypeElement, List<T>> map, TypeElement element) {
        List<T> list = map.get(element);
        if (list == null) {
            list = new ArrayList<>();
            map.put(element, list);
        }
        return list;
    }

    private Set<PackageElement> packageSet(TypeElement te) {
        Set<PackageElement> pkgSet = classToPackage.get(te);
        if (pkgSet == null) {
            pkgSet = new TreeSet<>(comparators.makeClassUseComparator());
            classToPackage.put(te, pkgSet);
        }
        return pkgSet;
    }

    private Set<TypeElement> classSet(TypeElement te) {
        Set<TypeElement> clsSet = classToClass.get(te);
        if (clsSet == null) {
            clsSet = new TreeSet<>(comparators.makeClassUseComparator());
            classToClass.put(te, clsSet);
        }
        return clsSet;
    }

    private <T extends Element> void add(Map<TypeElement, List<T>> map, TypeElement te, T ref) {
        // add to specified map
        refList(map, te).add(ref);
        // add ref's package to package map and class map
        packageSet(te).add(elementUtils.getPackageOf(ref));
        TypeElement entry = (utils.isField((Element) ref)
                || utils.isConstructor((Element) ref)
                || utils.isMethod((Element) ref))
                ? (TypeElement) ref.getEnclosingElement()
                : (TypeElement) ref;
        classSet(te).add(entry);
    }

    private void addAll(Map<TypeElement, List<TypeElement>> map, TypeElement te, Collection<TypeElement> refs) {
        if (refs == null) {
            return;
        }
        // add to specified map
        refList(map, te).addAll(refs);

        Set<PackageElement> pkgSet = packageSet(te);
        Set<TypeElement> clsSet = classSet(te);
        // add ref's package to package map and class map
        for (TypeElement cls : refs) {
            pkgSet.add(utils.containingPackage(cls));
            clsSet.add(cls);
        }
    }

    /**
     * Map the TypeElements to the members that use them as type parameters.
     *
     * @param map the map the insert the information into.
     * @param element the te whose type parameters are being checked.
     * @param holder the holder that owns the type parameters.
     */
    private <T extends Element> void mapTypeParameters(final Map<TypeElement, List<T>> map,
            Element element, final T holder) {

        SimpleElementVisitor14<Void, Void> elementVisitor
                = new SimpleElementVisitor14<Void, Void>() {

                    private void addParameters(TypeParameterElement e) {
                        for (TypeMirror type : utils.getBounds(e)) {
                            addTypeParameterToMap(map, type, holder);
                        }
                    }

                    @Override
                    public Void visitType(TypeElement e, Void p) {
                        for (TypeParameterElement param : e.getTypeParameters()) {
                            addParameters(param);
                        }
                        return null;
                    }

                    @Override
                    public Void visitExecutable(ExecutableElement e, Void p) {
                        for (TypeParameterElement param : e.getTypeParameters()) {
                            addParameters(param);
                        }
                        return null;
                    }

                    @Override
                    protected Void defaultAction(Element e, Void p) {
                        mapTypeParameters(map, e.asType(), holder);
                        return null;
                    }

                    @Override
                    public Void visitTypeParameter(TypeParameterElement e, Void p) {
                        addParameters(e);
                        return null;
                    }
                };
        elementVisitor.visit(element);
    }

    private <T extends Element> void mapTypeParameters(final Map<TypeElement, List<T>> map,
            TypeMirror aType, final T holder) {

        SimpleTypeVisitor9<Void, Void> tv = new SimpleTypeVisitor9<Void, Void>() {

            @Override
            public Void visitWildcard(WildcardType t, Void p) {
                TypeMirror bound = t.getExtendsBound();
                if (bound != null) {
                    addTypeParameterToMap(map, bound, holder);
                }
                bound = t.getSuperBound();
                if (bound != null) {
                    addTypeParameterToMap(map, bound, holder);
                }
                return null;
            }

            // ParameterizedType
            @Override
            public Void visitDeclared(DeclaredType t, Void p) {
                for (TypeMirror targ : t.getTypeArguments()) {
                    addTypeParameterToMap(map, targ, holder);
                }
                return null;
            }
        };
        tv.visit(aType);
    }

    /**
     * Map the AnnotationType to the members that use them as type parameters.
     *
     * @param map the map the insert the information into.
     * @param e whose type parameters are being checked.
     * @param holder owning the type parameters.
     */
    private <T extends Element> void mapAnnotations(final Map<TypeElement, List<T>> map,
            Element e, final T holder) {
        new SimpleElementVisitor14<Void, Void>() {

            void addAnnotations(Element e) {
                for (AnnotationMirror a : e.getAnnotationMirrors()) {
                    add(map, (TypeElement) a.getAnnotationType().asElement(), holder);
                }
            }

            @Override
            public Void visitPackage(PackageElement e, Void p) {
                for (AnnotationMirror a : e.getAnnotationMirrors()) {
                    refList(map, (TypeElement) a.getAnnotationType().asElement()).add(holder);
                }
                return null;
            }

            @Override
            protected Void defaultAction(Element e, Void p) {
                addAnnotations(e);
                return null;
            }
        }.visit(e);
    }

    private <T extends Element> void addTypeParameterToMap(final Map<TypeElement, List<T>> map,
            TypeMirror type, final T holder) {
        new SimpleTypeVisitor9<Void, Void>() {

            @Override
            protected Void defaultAction(TypeMirror e, Void p) {
                return super.defaultAction(e, p);
            }

            @Override
            public Void visitDeclared(DeclaredType t, Void p) {
                add(map, (TypeElement) t.asElement(), holder);
                return null;
            }

        }.visit(type);
        mapTypeParameters(map, type, holder);
    }
}
