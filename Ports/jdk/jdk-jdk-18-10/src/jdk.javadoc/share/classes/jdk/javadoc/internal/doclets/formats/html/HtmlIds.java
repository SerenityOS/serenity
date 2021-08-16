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

package jdk.javadoc.internal.doclets.formats.html;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor9;

import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.toolkit.util.SummaryAPIListBuilder;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;

/**
 * Centralized constants and factory methods for HTML ids.
 *
 * <p>To ensure consistency, these constants and methods should be used
 * both when declaring ids (for example, {@code HtmlTree.setId})
 * and creating references (for example, {@code Links.createLink}).
 *
 * <p>Most ids are mostly for internal use within the pages of a documentation
 * bundle. However, the ids for member declarations may be referred to
 * from other documentation using {@code {@link}}, and should not be
 * changed without due consideration for the compatibility impact.
 *
 * <p>The use of punctuating characters is inconsistent and could be improved.
 *
 * <p>Constants and methods are {@code static} where possible.
 * However, some methods require access to {@code utils} and are
 * better provided as instance methods.
 */
public class HtmlIds {
    private final HtmlConfiguration configuration;
    private final Utils utils;

    static final HtmlId ALL_CLASSES_TABLE = HtmlId.of("all-classes-table");
    static final HtmlId ALL_MODULES_TABLE = HtmlId.of("all-modules-table");
    static final HtmlId ALL_PACKAGES_TABLE = HtmlId.of("all-packages-table");
    static final HtmlId ANNOTATION_TYPE_ELEMENT_DETAIL = HtmlId.of("annotation-interface-element-detail");
    static final HtmlId ANNOTATION_TYPE_OPTIONAL_ELEMENT_SUMMARY = HtmlId.of("annotation-interface-optional-element-summary");
    static final HtmlId ANNOTATION_TYPE_REQUIRED_ELEMENT_SUMMARY = HtmlId.of("annotation-interface-required-element-summary");
    static final HtmlId CLASS_DESCRIPTION = HtmlId.of("class-description");
    static final HtmlId CLASS_SUMMARY = HtmlId.of("class-summary");
    static final HtmlId CONSTRUCTOR_DETAIL = HtmlId.of("constructor-detail");
    static final HtmlId CONSTRUCTOR_SUMMARY = HtmlId.of("constructor-summary");
    static final HtmlId ENUM_CONSTANT_DETAIL = HtmlId.of("enum-constant-detail");
    static final HtmlId ENUM_CONSTANT_SUMMARY = HtmlId.of("enum-constant-summary");
    static final HtmlId FIELD_DETAIL = HtmlId.of("field-detail");
    static final HtmlId FIELD_SUMMARY = HtmlId.of("field-summary");
    static final HtmlId FOR_REMOVAL = HtmlId.of("for-removal");
    static final HtmlId HELP_NAVIGATION = HtmlId.of("help-navigation");
    static final HtmlId HELP_PAGES = HtmlId.of("help-pages");
    static final HtmlId HELP_SEARCH = HtmlId.of("help-search");
    static final HtmlId METHOD_DETAIL = HtmlId.of("method-detail");
    static final HtmlId METHOD_SUMMARY = HtmlId.of("method-summary");
    static final HtmlId METHOD_SUMMARY_TABLE = HtmlId.of("method-summary-table");
    static final HtmlId MODULES = HtmlId.of("modules-summary");
    static final HtmlId MODULE_DESCRIPTION = HtmlId.of("module-description");
    static final HtmlId NAVBAR_TOP = HtmlId.of("navbar-top");
    static final HtmlId NAVBAR_TOP_FIRSTROW = HtmlId.of("navbar-top-firstrow");
    static final HtmlId NESTED_CLASS_SUMMARY = HtmlId.of("nested-class-summary");
    static final HtmlId PACKAGES = HtmlId.of("packages-summary");
    static final HtmlId PACKAGE_DESCRIPTION = HtmlId.of("package-description");
    static final HtmlId PACKAGE_SUMMARY_TABLE = HtmlId.of("package-summary-table");
    static final HtmlId PROPERTY_DETAIL = HtmlId.of("property-detail");
    static final HtmlId PROPERTY_SUMMARY = HtmlId.of("property-summary");
    static final HtmlId RELATED_PACKAGE_SUMMARY = HtmlId.of("related-package-summary");
    static final HtmlId RESET_BUTTON = HtmlId.of("reset-button");
    static final HtmlId SEARCH_INPUT = HtmlId.of("search-input");
    static final HtmlId SERVICES = HtmlId.of("services-summary");
    static final HtmlId SKIP_NAVBAR_TOP = HtmlId.of("skip-navbar-top");
    static final HtmlId UNNAMED_PACKAGE_ANCHOR = HtmlId.of("unnamed-package");

    private static final String ENUM_CONSTANTS_INHERITANCE = "enum-constants-inherited-from-class-";
    private static final String FIELDS_INHERITANCE = "fields-inherited-from-class-";
    private static final String METHODS_INHERITANCE = "methods-inherited-from-class-";
    private static final String NESTED_CLASSES_INHERITANCE = "nested-classes-inherited-from-class-";
    private static final String PROPERTIES_INHERITANCE = "properties-inherited-from-class-";

    /**
     * Creates a factory for element-specific ids.
     *
     * @param configuration the configuration
     */
    HtmlIds(HtmlConfiguration configuration) {
        this.configuration = configuration;
        this.utils = configuration.utils;
    }

    /**
     * Returns an id for a package.
     *
     * @param element the package
     *
     * @return the id
     */
    HtmlId forPackage(PackageElement element) {
        return element == null || element.isUnnamed()
                ? UNNAMED_PACKAGE_ANCHOR
                : HtmlId.of(element.getQualifiedName().toString());
    }

    /**
     * Returns an id for a class or interface.
     *
     * @param element the class or interface
     *
     * @return the id
     */
    HtmlId forClass(TypeElement element) {
        return HtmlId.of(utils.getFullyQualifiedName(element));
    }

    /**
     * Returns an id for an executable element, suitable for use when the
     * simple name and argument list will be unique within the page, such as
     * in the page for the declaration of the enclosing class or interface.
     *
     * @param element the element
     *
     * @return the id
     */
    HtmlId forMember(ExecutableElement element) {
        String a = element.getSimpleName()
                        + utils.makeSignature(element, null, true, true);
        // utils.makeSignature includes spaces
        return HtmlId.of(a.replaceAll("\\s", ""));
    }

    /**
     * Returns an id for an executable element, including the context
     * of its documented enclosing class or interface.
     *
     * @param typeElement the enclosing class or interface
     * @param member      the element
     *
     * @return the id
     */
    HtmlId forMember(TypeElement typeElement, ExecutableElement member) {
        return HtmlId.of(utils.getSimpleName(member) + utils.signature(member, typeElement));
    }

    /**
     * Returns an id for a field, suitable for use when the simple name
     * will be unique within the page, such as in the page for the
     * declaration of the enclosing class or interface.
     *
     * <p>Warning: the name may not be unique if a property with the same
     * name is also being documented in the same class.
     *
     * @param element the element
     *
     * @return the id
     *
     * @see #forProperty(ExecutableElement)
     */
    HtmlId forMember(VariableElement element) {
        return HtmlId.of(element.getSimpleName().toString());
    }

    /**
     * Returns an id for a field, including the context
     * of its documented enclosing class or interface.
     *
     * @param typeElement the enclosing class or interface
     * @param member the element
     *
     * @return the id
     */
    HtmlId forMember(TypeElement typeElement, VariableElement member) {
        return HtmlId.of(typeElement.getQualifiedName() + "." + member.getSimpleName());
    }

    /**
     * Returns an id for the erasure of an executable element,
     * or {@code null} if there are no type variables in the signature.
     *
     * For backward compatibility, include an anchor using the erasures of the
     * parameters.  NOTE:  We won't need this method anymore after we fix
     * {@code @see} tags so that they use the type instead of the erasure.
     *
     * @param executableElement the element to anchor to
     * @return the 1.4.x style anchor for the executable element
     */
    protected HtmlId forErasure(ExecutableElement executableElement) {
        final StringBuilder buf = new StringBuilder(executableElement.getSimpleName().toString());
        buf.append("(");
        List<? extends VariableElement> parameters = executableElement.getParameters();
        boolean foundTypeVariable = false;
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                buf.append(",");
            }
            TypeMirror t = parameters.get(i).asType();
            SimpleTypeVisitor9<Boolean, Void> stv = new SimpleTypeVisitor9<>() {
                boolean foundTypeVariable = false;

                @Override
                public Boolean visitArray(ArrayType t, Void p) {
                    visit(t.getComponentType());
                    buf.append(utils.getDimension(t));
                    return foundTypeVariable;
                }

                @Override
                public Boolean visitTypeVariable(TypeVariable t, Void p) {
                    buf.append(utils.asTypeElement(t).getQualifiedName().toString());
                    foundTypeVariable = true;
                    return foundTypeVariable;
                }

                @Override
                public Boolean visitDeclared(DeclaredType t, Void p) {
                    buf.append(utils.getQualifiedTypeName(t));
                    return foundTypeVariable;
                }

                @Override
                protected Boolean defaultAction(TypeMirror e, Void p) {
                    buf.append(e);
                    return foundTypeVariable;
                }
            };

            boolean isTypeVariable = stv.visit(t);
            if (!foundTypeVariable) {
                foundTypeVariable = isTypeVariable;
            }
        }
        buf.append(")");
        return foundTypeVariable ? HtmlId.of(buf.toString()) : null;
    }

    /**
     * Returns an id for a property, suitable for use when the simple name
     * will be unique within the page, such as in the page for the
     * declaration of the enclosing class or interface.
     *
     * <p>Warning: the name may not be unique if a field with the same
     * name is also being documented in the same class.
     *
     * @param element the element
     *
     * @return the id
     *
     * @see #forMember(VariableElement)
     */
    HtmlId forProperty(ExecutableElement element) {
        return HtmlId.of(element.getSimpleName().toString());
    }

    /**
     * Returns an id for the list of classes and interfaces inherited from
     * a class or interface.
     *
     * <p>Note: the use of {@code utils} may not be strictly necessary.
     *
     * @param element the class or interface
     *
     * @return the id
     */
    HtmlId forInheritedClasses(TypeElement element) {
        return HtmlId.of(NESTED_CLASSES_INHERITANCE + utils.getFullyQualifiedName(element));
    }

    /**
     * Returns an id for the list of fields inherited from a class or interface.
     *
     * @param element the class or interface
     *
     * @return the id
     */
    HtmlId forInheritedFields(TypeElement element) {
        return forInherited(FIELDS_INHERITANCE, element);
    }

    /**
     * Returns an id for the list of enum constants inherited from a class or interface.
     *
     * @param element the class or interface
     *
     * @return the id
     */
    HtmlId forInheritedEnumConstants(TypeElement element) {
        return forInherited(ENUM_CONSTANTS_INHERITANCE, element);
    }

    /**
     * Returns an id for the list of methods inherited from a class or interface.
     *
     * @param element the class or interface
     *
     * @return the id
     */
    HtmlId forInheritedMethods(TypeElement element) {
        return forInherited(METHODS_INHERITANCE, element);
    }

    /**
     * Returns an id for the list of properties inherited from a class or interface.
     *
     * @param element the class or interface
     *
     * @return the id
     */
    HtmlId forInheritedProperties(TypeElement element) {
        return forInherited(PROPERTIES_INHERITANCE, element);
    }

    // Note: the use of {@code configuration} may not be strictly necessary as
    // compared to just using the fully qualified name, but would be a change in the value.
    private HtmlId forInherited(String prefix, TypeElement element) {
        return HtmlId.of(prefix + configuration.getClassName(element));
    }

    /**
     * Returns an id for a character on the A-Z Index page.
     *
     * @param character the character
     *
     * @return the id
     */
    static HtmlId forIndexChar(char character) {
        return HtmlId.of("I:" + character);
    }

    /**
     * Returns an id for a line in a source-code listing.
     *
     * @param lineNumber the line number
     *
     * @return the id
     */
    static HtmlId forLine(int lineNumber) {
        return HtmlId.of("line-" + lineNumber);
    }

    /**
     * Returns an id for a parameter, such as a component of a record.
     *
     * <p>Warning: this may not be unique on the page if used when there are
     * other like-named parameters.
     *
     * @param paramName the parameter name
     *
     * @return the id
     */
    static HtmlId forParam(String paramName) {
        return HtmlId.of("param-" + paramName);
    }

    /**
     * Returns an id for a fragment of text, such as in an {@code @index} tag,
     * using a map of counts to ensure the id is unique.
     *
     * @param text the text
     * @param counts the map of counts
     *
     * @return the id
     */
    static HtmlId forText(String text, Map<String, Integer> counts) {
        String base = text.replaceAll("\\s+", "");
        int count = counts.compute(base, (k, v) -> v == null ? 0 : v + 1);
        return HtmlId.of(count == 0 ? base : base + "-" + count);
    }

    /**
     * Returns an id for one of the kinds of section in the pages for item group summaries.
     *
     * <p>Note: while the use of simple names (that are not keywords)
     * may seem undesirable, they cannot conflict with the unqualified names
     * of fields and properties, which should not also appear on the same page.
     *
     * @param kind the kind of deprecated items in the section
     *
     * @return the id
     */
    static HtmlId forSummaryKind(SummaryAPIListBuilder.SummaryElementKind kind) {
        return HtmlId.of(switch (kind) {
            case MODULE -> "module";
            case PACKAGE -> "package";
            case INTERFACE -> "interface";
            case CLASS -> "class";
            case ENUM -> "enum-class";
            case EXCEPTION -> "exception";
            case ERROR -> "error";
            case ANNOTATION_TYPE -> "annotation-interface";
            case FIELD -> "field";
            case METHOD -> "method";
            case CONSTRUCTOR -> "constructor";
            case ENUM_CONSTANT -> "enum-constant";
            case ANNOTATION_TYPE_MEMBER -> "annotation-interface-member";
            case RECORD_CLASS -> "record-class";
        });
    }

    /**
     * Returns an id for the member summary table of the given {@code kind} in a class page.
     *
     * @param kind the kind of member
     *
     * @return the id
     */
    static HtmlId forMemberSummary(VisibleMemberTable.Kind kind) {
        return switch (kind) {
            case NESTED_CLASSES -> NESTED_CLASS_SUMMARY;
            case ENUM_CONSTANTS -> ENUM_CONSTANT_SUMMARY;
            case FIELDS -> FIELD_SUMMARY;
            case CONSTRUCTORS -> CONSTRUCTOR_SUMMARY;
            case METHODS -> METHOD_SUMMARY;
            case ANNOTATION_TYPE_MEMBER_OPTIONAL -> ANNOTATION_TYPE_OPTIONAL_ELEMENT_SUMMARY;
            case ANNOTATION_TYPE_MEMBER_REQUIRED -> ANNOTATION_TYPE_REQUIRED_ELEMENT_SUMMARY;
            case PROPERTIES -> PROPERTY_SUMMARY;
        };
    }

    /**
     * Returns an id for a "tab" in a table.
     *
     * @param tableId the id for the table
     * @param tabIndex the index of the tab
     *
     * @return the id
     */
    public static HtmlId forTab(HtmlId tableId, int tabIndex) {
        return HtmlId.of(tableId.name() + "-tab" + tabIndex);
    }

    /**
     * Returns an id for the "tab panel" in a table.
     *
     * @param tableId the id for the table
     *
     * @return the id
     */
    public static HtmlId forTabPanel(HtmlId tableId) {
        return HtmlId.of(tableId.name() + ".tabpanel");
    }


    /**
     * Returns an id for the "preview" section for an element.
     *
     * @param el the element
     *
     * @return the id
     */
    public HtmlId forPreviewSection(Element el) {
        return HtmlId.of("preview-" + switch (el.getKind()) {
            case CONSTRUCTOR, METHOD -> forMember((ExecutableElement) el).name();
            case PACKAGE -> forPackage((PackageElement) el).name();
            default -> utils.getFullyQualifiedName(el, false);
        });
    }

    /**
     * Returns an id for the entry on the HELP page for a kind of generated page.
     *
     * @param page the kind of page
     *
     * @return the id
     */
    public HtmlId forPage(Navigation.PageMode page) {
        return HtmlId.of(page.name().toLowerCase(Locale.ROOT).replace("_", "-"));
    }
}
