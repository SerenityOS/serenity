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

package jdk.javadoc.internal.doclets.toolkit.util;

import com.sun.source.doctree.SerialFieldTree;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor9;
import java.util.Comparator;
import java.util.List;

/**
 *  A collection of {@code Comparator} factory methods.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Comparators {

    private final Utils utils;

    Comparators(Utils utils) {
        this.utils = utils;
    }

    private Comparator<Element> moduleComparator = null;

    /**
     * Comparator for ModuleElements, simply compares the fully qualified names
     * @return a Comparator
     */
    public Comparator<Element> makeModuleComparator() {
        if (moduleComparator == null) {
            moduleComparator = new ElementComparator() {
                @Override
                public int compare(Element mod1, Element mod2) {
                    return compareFullyQualifiedNames(mod1, mod2);
                }
            };
        }
        return moduleComparator;
    }

    private Comparator<Element> allClassesComparator = null;

    /**
     * Returns a Comparator for all classes, compares the simple names of
     * TypeElement, if equal then the fully qualified names, and if equal again
     * the names of the enclosing modules.
     *
     * @return Comparator
     */
    public Comparator<Element> makeAllClassesComparator() {
        if (allClassesComparator == null) {
            allClassesComparator = new ElementComparator() {
                @Override
                public int compare(Element e1, Element e2) {
                    int result = compareNames(e1, e2);
                    if (result == 0)
                        result = compareFullyQualifiedNames(e1, e2);
                    if (result == 0)
                        result = compareModuleNames(e1, e2);
                    return result;
                }
            };
        }
        return allClassesComparator;
    }

    private Comparator<Element> packageComparator = null;

    /**
     * Returns a Comparator for packages, by comparing the fully qualified names,
     * and if those are equal the names of the enclosing modules.
     *
     * @return a Comparator
     */
    public Comparator<Element> makePackageComparator() {
        if (packageComparator == null) {
            packageComparator = new ElementComparator() {
                @Override
                public int compare(Element pkg1, Element pkg2) {
                    int result = compareFullyQualifiedNames(pkg1, pkg2);
                    if (result == 0)
                        result = compareModuleNames(pkg1, pkg2);
                    return result;
                }
            };
        }
        return packageComparator;
    }

    private Comparator<Element> summaryComparator = null;

    /**
     * Returns a Comparator for items listed on summary list pages
     * (like deprecated or preview summary pages), by comparing the
     * fully qualified names, and if those are equal the names of the enclosing modules.
     *
     * @return a Comparator
     */
    public Comparator<Element> makeSummaryComparator() {
        if (summaryComparator == null) {
            summaryComparator = new ElementComparator() {
                @Override
                public int compare(Element e1, Element e2) {
                    int result = compareFullyQualifiedNames(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    // if elements are executable compare their parameter arrays
                    result = compareParameters(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    return compareModuleNames(e1, e2);
                }
            };
        }
        return summaryComparator;
    }

    private Comparator<SerialFieldTree> serialFieldTreeComparator = null;

    /**
     * Returns a Comparator for SerialFieldTree.
     * @return a Comparator
     */
    public Comparator<SerialFieldTree> makeSerialFieldTreeComparator() {
        if (serialFieldTreeComparator == null) {
            serialFieldTreeComparator = (SerialFieldTree o1, SerialFieldTree o2) -> {
                String s1 = o1.getName().toString();
                String s2 = o2.getName().toString();
                return s1.compareTo(s2);
            };
        }
        return serialFieldTreeComparator;
    }

    /**
     * Returns a general purpose comparator.
     * @return a Comparator
     */
    public Comparator<Element> makeGeneralPurposeComparator() {
        return makeClassUseComparator();
    }

    private Comparator<Element> overrideUseComparator = null;

    /**
     * Returns a Comparator for overrides and implements,
     * used primarily on methods, compares the name first,
     * then compares the simple names of the enclosing
     * TypeElement and the fully qualified name of the enclosing TypeElement.
     * @return a Comparator
     */
    public Comparator<Element> makeOverrideUseComparator() {
        if (overrideUseComparator == null) {
            overrideUseComparator = new ElementComparator() {
                @Override
                public int compare(Element o1, Element o2) {
                    int result = utils.compareStrings(utils.getSimpleName(o1), utils.getSimpleName(o2));
                    if (result != 0) {
                        return result;
                    }
                    if (!utils.isTypeElement(o1) && !utils.isTypeElement(o2) && !utils.isPackage(o1) && !utils.isPackage(o2)) {
                        TypeElement t1 = utils.getEnclosingTypeElement(o1);
                        TypeElement t2 = utils.getEnclosingTypeElement(o2);
                        result = utils.compareStrings(utils.getSimpleName(t1), utils.getSimpleName(t2));
                        if (result != 0)
                            return result;
                    }
                    result = utils.compareStrings(utils.getFullyQualifiedName(o1), utils.getFullyQualifiedName(o2));
                    if (result != 0)
                        return result;
                    return compareElementKinds(o1, o2);
                }
            };
        }
        return overrideUseComparator;
    }

    private Comparator<Element> indexUseComparator = null;

    /**
     *  Returns an {@code Element} Comparator for index file presentations, and are sorted as follows.
     *  If comparing modules and/or packages then simply compare the qualified names,
     *  if comparing a module or a package with a type/member then compare the
     *  FullyQualifiedName of the module or a package with the SimpleName of the entity,
     *  otherwise:
     *  1. compare the ElementKind ex: Module, Package, Interface etc.
     *  2a. if equal and if the type is of ExecutableElement(Constructor, Methods),
     *      a case insensitive comparison of parameter the type signatures
     *  2b. if equal, case sensitive comparison of the type signatures
     *  3. if equal, compare the FQNs of the entities
     *  4. finally, if equal, compare the names of the enclosing modules
     * @return an element comparator for index file use
     */
    public Comparator<Element> makeIndexElementComparator() {
        if (indexUseComparator == null) {
            indexUseComparator = new ElementComparator() {
                /**
                 * Compares two elements.
                 *
                 * @param e1 - an element.
                 * @param e2 - an element.
                 * @return a negative integer, zero, or a positive integer as the first
                 * argument is less than, equal to, or greater than the second.
                 */
                @Override
                public int compare(Element e1, Element e2) {
                    int result;
                    // first, compare names as appropriate
                    if ((utils.isModule(e1) || utils.isPackage(e1)) && (utils.isModule(e2) || utils.isPackage(e2))) {
                        result = compareFullyQualifiedNames(e1, e2);
                    } else if (utils.isModule(e1) || utils.isPackage(e1)) {
                        result = utils.compareStrings(utils.getFullyQualifiedName(e1), utils.getSimpleName(e2));
                    } else if (utils.isModule(e2) || utils.isPackage(e2)) {
                        result = utils.compareStrings(utils.getSimpleName(e1), utils.getFullyQualifiedName(e2));
                    } else {
                        result = compareNames(e1, e2);
                    }
                    if (result != 0) {
                        return result;
                    }
                    // if names are the same, compare element kinds
                    result = compareElementKinds(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    // if element kinds are the same, and are executable,
                    // compare the parameter arrays
                    result = compareParameters(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    // else fall back on fully qualified names
                    result = compareFullyQualifiedNames(e1, e2);
                    if (result != 0)
                        return result;
                    return compareModuleNames(e1, e2);
                }
            };
        }
        return indexUseComparator;
    }

    private Comparator<TypeMirror> typeMirrorClassUseComparator = null;

    /**
     * Returns a comparator that compares the fully qualified names of two type mirrors.
     *
     * @return the comparator
     */
    public Comparator<TypeMirror> makeTypeMirrorClassUseComparator() {
        if (typeMirrorClassUseComparator == null) {
            typeMirrorClassUseComparator = (TypeMirror type1, TypeMirror type2) -> {
                String s1 = utils.getQualifiedTypeName(type1);
                String s2 = utils.getQualifiedTypeName(type2);
                return utils.compareStrings(s1, s2);
            };
        }
        return typeMirrorClassUseComparator;
    }

    private Comparator<TypeMirror> typeMirrorIndexUseComparator = null;

    /**
     * Returns a comparator that compares the simple names of two type mirrors,
     * or the fully qualified names if the simple names are equal.
     *
     * @return the comparator
     */
    public Comparator<TypeMirror> makeTypeMirrorIndexUseComparator() {
        if (typeMirrorIndexUseComparator == null) {
            typeMirrorIndexUseComparator = (TypeMirror t1, TypeMirror t2) -> {
                int result = utils.compareStrings(utils.getTypeName(t1, false), utils.getTypeName(t2, false));
                if (result != 0)
                    return result;
                return utils.compareStrings(utils.getQualifiedTypeName(t1), utils.getQualifiedTypeName(t2));
            };
        }
        return typeMirrorIndexUseComparator;
    }

    private Comparator<Element> classUseComparator = null;

    /**
     * Comparator for ClassUse presentations, and sorts as follows:
     * 1. member names
     * 2. then fully qualified member names
     * 3. then parameter types if applicable
     * 4. the element kinds ie. package, class, interface etc.
     * 5. finally the name of the enclosing modules
     * @return a comparator to sort classes and members for class use
     */
    public Comparator<Element> makeClassUseComparator() {
        if (classUseComparator == null) {
            classUseComparator = new ElementComparator() {
                /**
                 * Compares two Elements.
                 *
                 * @param e1 - an element.
                 * @param e2 - an element.
                 * @return a negative integer, zero, or a positive integer as the first
                 * argument is less than, equal to, or greater than the second.
                 */
                @Override
                public int compare(Element e1, Element e2) {
                    int result = compareNames(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    result = compareFullyQualifiedNames(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    result = compareParameters(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    result = compareElementKinds(e1, e2);
                    if (result != 0) {
                        return result;
                    }
                    return compareModuleNames(e1, e2);
                }
            };
        }
        return classUseComparator;
    }

    /**
     * A general purpose comparator to sort Element entities, basically provides the building blocks
     * for creating specific comparators for an use-case.
     */
    private abstract class ElementComparator implements Comparator<Element> {
        public ElementComparator() { }

        /**
         * compares two parameter arrays by first comparing the length of the arrays, and
         * then each Type of the parameter in the array.
         * @param params1 the first parameter array.
         * @param params2 the first parameter array.
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        protected int compareParameters(boolean caseSensitive, List<? extends VariableElement> params1,
                                        List<? extends VariableElement> params2) {

            return utils.compareStrings(caseSensitive, getParametersAsString(params1),
                    getParametersAsString(params2));
        }

        String getParametersAsString(List<? extends VariableElement> params) {
            StringBuilder sb = new StringBuilder();
            for (VariableElement param : params) {
                TypeMirror t = param.asType();
                // prefix P for primitive and R for reference types, thus items will
                // be ordered lexically and correctly.
                sb.append(getTypeCode(t)).append("-").append(t).append("-");
            }
            return sb.toString();
        }

        private String getTypeCode(TypeMirror t) {
            return new SimpleTypeVisitor9<String, Void>() {

                @Override
                public String visitPrimitive(PrimitiveType t, Void p) {
                    return "P";
                }
                @Override
                public String visitArray(ArrayType t, Void p) {
                    return visit(t.getComponentType());
                }
                @Override
                protected String defaultAction(TypeMirror e, Void p) {
                    return "R";
                }

            }.visit(t);
        }

        /**
         * Compares two Elements, typically the name of a method,
         * field or constructor.
         * @param e1 the first Element.
         * @param e2 the second Element.
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        protected int compareNames(Element e1, Element e2) {
            return utils.compareStrings(utils.getSimpleName(e1), utils.getSimpleName(e2));
        }

        /**
         * Compares the fully qualified names of the entities
         * @param e1 the first Element.
         * @param e2 the first Element.
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        protected int compareFullyQualifiedNames(Element e1, Element e2) {
            // add simple name to be compatible
            String thisElement = getFullyQualifiedName(e1);
            String thatElement = getFullyQualifiedName(e2);
            return utils.compareStrings(thisElement, thatElement);
        }

        /**
         * Compares the name of the modules of two elements.
         * @param e1 the first element
         * @param e2 the second element
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second
         */
        protected int compareModuleNames(Element e1, Element e2) {
            ModuleElement m1 = utils.elementUtils.getModuleOf(e1);
            ModuleElement m2 = utils.elementUtils.getModuleOf(e2);
            if (m1 != null && m2 != null) {
                return compareFullyQualifiedNames(m1, m2);
            } else if (m1 != null) {
                return 1;
            } else if (m2 != null) {
                return -1;
            }
            return 0;
        }

        /**
         * Compares the parameter arrays of two elements if they both are executable.
         * @param e1 the first element
         * @param e2 the second element
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second
         */
        protected int compareParameters(Element e1, Element e2) {
            int result = 0;
            if (hasParameters(e1) && hasParameters(e2)) {
                List<? extends VariableElement> parameters1 = ((ExecutableElement)e1).getParameters();
                List<? extends VariableElement> parameters2 = ((ExecutableElement)e2).getParameters();
                result = compareParameters(false, parameters1, parameters2);
                if (result != 0) {
                    return result;
                }
                result = compareParameters(true, parameters1, parameters2);
            }
            return result;
        }

        /**
         * Compares the kinds of two elements.
         * @param e1 the first element
         * @param e2 the second element
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second
         */
        protected int compareElementKinds(Element e1, Element e2) {
            return Integer.compare(getKindIndex(e1), getKindIndex(e2));
        }

        private int getKindIndex(Element e) {
            return switch (e.getKind()) {
                case MODULE ->          0;
                case PACKAGE ->         1;
                case CLASS ->           2;
                case ENUM ->            3;
                case ENUM_CONSTANT ->   4;
                case RECORD ->          5;
                case INTERFACE ->       6;
                case ANNOTATION_TYPE -> 7;
                case FIELD ->           8;
                case CONSTRUCTOR ->     9;
                case METHOD ->          10;
                default -> throw new IllegalArgumentException(e.getKind().toString());
            };
        }

        boolean hasParameters(Element e) {
            return new SimpleElementVisitor14<Boolean, Void>() {
                @Override
                public Boolean visitExecutable(ExecutableElement e, Void p) {
                    return true;
                }

                @Override
                protected Boolean defaultAction(Element e, Void p) {
                    return false;
                }

            }.visit(e);
        }

        /**
         * The fully qualified names of the entities, used solely by the comparator.
         *
         * @return a negative integer, zero, or a positive integer as the first argument is less
         * than, equal to, or greater than the second.
         */
        private String getFullyQualifiedName(Element e) {
            return new SimpleElementVisitor14<String, Void>() {
                @Override
                public String visitModule(ModuleElement e, Void p) {
                    return e.getQualifiedName().toString();
                }

                @Override
                public String visitPackage(PackageElement e, Void p) {
                    return e.getQualifiedName().toString();
                }

                @Override
                public String visitExecutable(ExecutableElement e, Void p) {
                    // For backward compatibility
                    return getFullyQualifiedName(e.getEnclosingElement())
                            + "." + e.getSimpleName().toString();
                }

                @Override
                public String visitType(TypeElement e, Void p) {
                    return e.getQualifiedName().toString();
                }

                @Override
                protected String defaultAction(Element e, Void p) {
                    return utils.getEnclosingTypeElement(e).getQualifiedName().toString()
                            + "." + e.getSimpleName().toString();
                }
            }.visit(e);
        }
    }
}
