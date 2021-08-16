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

package jdk.javadoc.internal.doclets.formats.html;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;


/**
 * Constants and factory methods for common fragments of content
 * used by HtmlDoclet. The string content of these fragments is
 * generally obtained from the {@link Resources resources} found
 * in the doclet's configuration.
 *
 * @implNote
 * Many constants are made available in this class, so that they are
 * only created once per doclet-instance, instead of once per generated page.
 */
public class Contents {

    public final Content allClassesAndInterfacesLabel;
    public final Content allImplementedInterfacesLabel;
    public final Content allModulesLabel;
    public final Content allPackagesLabel;
    public final Content allSuperinterfacesLabel;
    public final Content also;
    public final Content annotationTypeOptionalMemberLabel;
    public final Content annotationTypeRequiredMemberLabel;
    public final Content annotateTypeOptionalMemberSummaryLabel;
    public final Content annotateTypeRequiredMemberSummaryLabel;
    public final Content annotationType;
    public final Content annotationTypeDetailsLabel;
    public final Content annotationTypeMemberDetail;
    public final Content annotationtypes;
    public final Content annotationTypes;
    public final Content classLabel;
    public final Content classes;
    public final Content constantFieldLabel;
    public final Content constantsSummaryTitle;
    public final Content constructorLabel;
    public final Content constructorDetailsLabel;
    public final Content constructorSummaryLabel;
    public final Content constructors;
    public final Content contentsHeading;
    public final Content defaultPackageLabel;
    public final Content default_;
    public final Content deprecatedAPI;
    public final Content deprecatedLabel;
    public final Content deprecatedPhrase;
    public final Content deprecatedForRemovalPhrase;
    public final Content descfrmClassLabel;
    public final Content descfrmInterfaceLabel;
    public final Content descriptionLabel;
    public final Content detailLabel;
    public final Content enclosingClassLabel;
    public final Content enclosingInterfaceLabel;
    public final Content enumConstantLabel;
    public final Content enumConstantDetailLabel;
    public final Content enumConstantSummary;
    public final Content enum_;
    public final Content enums;
    public final Content error;
    public final Content errors;
    public final Content exception;
    public final Content exceptions;
    public final Content exportedTo;
    public final Content fieldLabel;
    public final Content fieldDetailsLabel;
    public final Content fieldSummaryLabel;
    public final Content fields;
    public final Content fromLabel;
    public final Content functionalInterface;
    public final Content functionalInterfaceMessage;
    public final Content helpLabel;
    public final Content helpSubNavLabel;
    public final Content hierarchyForAllPackages;
    public final Content implementation;
    public final Content implementingClassesLabel;
    public final Content inClass;
    public final Content inInterface;
    public final Content indexLabel;
    public final Content interfaceLabel;
    public final Content interfaces;
    public final Content methodDetailLabel;
    public final Content methodLabel;
    public final Content methodSummary;
    public final Content methods;
    public final Content modifierAndTypeLabel;
    public final Content modifierLabel;
    public final Content moduleLabel;
    public final Content module_;
    public final Content moduleSubNavLabel;
    public final Content modulesLabel;
    public final Content navAnnotationTypeMember;
    public final Content navAnnotationTypeOptionalMember;
    public final Content navAnnotationTypeRequiredMember;
    public final Content navClassesAndInterfaces;
    public final Content navConstructor;
    public final Content navDescription;
    public final Content navEnum;
    public final Content navField;
    public final Content navHelpNavigation;
    public final Content navHelpPages;
    public final Content navMethod;
    public final Content navModules;
    public final Content navNested;
    public final Content navPackages;
    public final Content navProperty;
    public final Content navServices;
    public final Content nestedClassSummary;
    public final Content newAPI;
    public final Content newLabel;
    public final Content noScriptMessage;
    public final Content openModuleLabel;
    public final Content openedTo;
    public final Content overridesLabel;
    public final Content overviewLabel;
    public final Content packageHierarchies;
    public final Content packageLabel;
    public final Content package_;
    public final Content packagesLabel;
    public final Content packageSubNavLabel;
    public final Content packageSummaryLabel;
    public final Content parameters;
    public final Content previewAPI;
    public final Content previewLabel;
    public final Content previewMark;
    public final Content previewPhrase;
    public final Content properties;
    public final Content propertyLabel;
    public final Content propertyDetailsLabel;
    public final Content propertySummaryLabel;
    public final Content records;
    public final Content recordComponents;
    public final Content referencedIn;
    public final Content relatedPackages;
    public final Content returns;
    public final Content seeAlso;
    public final Content serializedForm;
    public final Content servicesLabel;
    public final Content specifiedByLabel;
    public final Content subclassesLabel;
    public final Content subinterfacesLabel;
    public final Content summaryLabel;
    public final Content systemPropertiesLabel;
    public final Content systemPropertiesSummaryLabel;
    public final Content throws_;
    public final Content treeLabel;
    public final Content typeLabel;
    public final Content typeParameters;
    public final Content useLabel;
    public final Content valueLabel;

    private final EnumMap<VisibleMemberTable.Kind, Content> navLinkLabels;

    private final Resources resources;

    /**
     * Creates a {@code Contents} object.
     *
     * @param configuration the configuration in which to find the
     * resources used to look up resource keys, and other details.
     */
    Contents(HtmlConfiguration configuration) {
        this.resources = configuration.getDocResources();

        this.allClassesAndInterfacesLabel = getContent("doclet.All_Classes_And_Interfaces");
        allImplementedInterfacesLabel = getContent("doclet.All_Implemented_Interfaces");
        allModulesLabel = getNonBreakResource("doclet.All_Modules");
        allPackagesLabel = getNonBreakResource("doclet.All_Packages");
        allSuperinterfacesLabel = getContent("doclet.All_Superinterfaces");
        also = getContent("doclet.also");
        annotationTypeOptionalMemberLabel = getContent("doclet.Annotation_Type_Optional_Member");
        annotationTypeRequiredMemberLabel = getContent("doclet.Annotation_Type_Required_Member");
        annotateTypeOptionalMemberSummaryLabel = getContent("doclet.Annotation_Type_Optional_Member_Summary");
        annotateTypeRequiredMemberSummaryLabel = getContent("doclet.Annotation_Type_Required_Member_Summary");
        annotationType = getContent("doclet.AnnotationType");
        annotationTypeDetailsLabel = getContent("doclet.Annotation_Type_Member_Detail");
        annotationTypeMemberDetail = getContent("doclet.Annotation_Type_Member_Detail");
        annotationTypes = getContent("doclet.AnnotationTypes");
        annotationtypes = getContent("doclet.annotationtypes");
        classLabel = getContent("doclet.Class");
        classes = getContent("doclet.Classes");
        constantFieldLabel = getContent("doclet.ConstantField");
        constantsSummaryTitle = getContent("doclet.Constants_Summary");
        constructorLabel = getContent("doclet.Constructor");
        constructorDetailsLabel = getContent("doclet.Constructor_Detail");
        constructorSummaryLabel = getContent("doclet.Constructor_Summary");
        constructors = getContent("doclet.Constructors");
        contentsHeading = getContent("doclet.Contents");
        defaultPackageLabel = getContent("doclet.Unnamed_Package");
        default_ = getContent("doclet.Default");
        deprecatedAPI = getContent("doclet.Deprecated_API");
        deprecatedLabel = getContent("doclet.navDeprecated");
        deprecatedPhrase = getContent("doclet.Deprecated");
        deprecatedForRemovalPhrase = getContent("doclet.DeprecatedForRemoval");
        descfrmClassLabel = getContent("doclet.Description_From_Class");
        descfrmInterfaceLabel = getContent("doclet.Description_From_Interface");
        descriptionLabel = getContent("doclet.Description");
        detailLabel = getContent("doclet.Detail");
        enclosingClassLabel = getContent("doclet.Enclosing_Class");
        enclosingInterfaceLabel = getContent("doclet.Enclosing_Interface");
        enumConstantLabel = getContent("doclet.Enum_Constant");
        enumConstantDetailLabel = getContent("doclet.Enum_Constant_Detail");
        enumConstantSummary = getContent("doclet.Enum_Constant_Summary");
        enum_ = getContent("doclet.Enum");
        enums = getContent("doclet.Enums");
        error = getContent("doclet.Error");
        errors = getContent("doclet.Errors");
        exception = getContent("doclet.Exception");
        exceptions = getContent("doclet.Exceptions");
        exportedTo = getContent("doclet.ExportedTo");
        fieldDetailsLabel = getContent("doclet.Field_Detail");
        fieldSummaryLabel = getContent("doclet.Field_Summary");
        fieldLabel = getContent("doclet.Field");
        fields = getContent("doclet.Fields");
        fromLabel = getContent("doclet.From");
        functionalInterface = getContent("doclet.Functional_Interface");
        functionalInterfaceMessage = getContent("doclet.Functional_Interface_Message");
        helpLabel = getContent("doclet.Help");
        helpSubNavLabel = getContent("doclet.Help_Sub_Nav");
        hierarchyForAllPackages = getContent("doclet.Hierarchy_For_All_Packages");
        implementation = getContent("doclet.Implementation");
        implementingClassesLabel = getContent("doclet.Implementing_Classes");
        inClass = getContent("doclet.in_class");
        inInterface = getContent("doclet.in_interface");
        indexLabel = getContent("doclet.Index");
        interfaceLabel = getContent("doclet.Interface");
        interfaces = getContent("doclet.Interfaces");
        methodDetailLabel = getContent("doclet.Method_Detail");
        methodSummary = getContent("doclet.Method_Summary");
        methodLabel = getContent("doclet.Method");
        methods = getContent("doclet.Methods");
        modifierLabel = getContent("doclet.Modifier");
        modifierAndTypeLabel = getContent("doclet.Modifier_and_Type");
        moduleLabel = getContent("doclet.Module");
        module_ = getContent("doclet.module");
        moduleSubNavLabel = getContent("doclet.Module_Sub_Nav");
        modulesLabel = getContent("doclet.Modules");
        navAnnotationTypeMember = getContent("doclet.navAnnotationTypeMember");
        navAnnotationTypeOptionalMember = getContent("doclet.navAnnotationTypeOptionalMember");
        navAnnotationTypeRequiredMember = getContent("doclet.navAnnotationTypeRequiredMember");
        navClassesAndInterfaces = getContent("doclet.navClassesAndInterfaces");
        navConstructor = getContent("doclet.navConstructor");
        navEnum = getContent("doclet.navEnum");
        navField = getContent("doclet.navField");
        navHelpNavigation = getContent("doclet.navNavigation");
        navHelpPages = getContent("doclet.navPages");
        navMethod = getContent("doclet.navMethod");
        navDescription = getContent("doclet.navDescription");
        navModules = getContent("doclet.navModules");
        navNested = getContent("doclet.navNested");
        navPackages = getContent("doclet.navPackages");
        navProperty = getContent("doclet.navProperty");
        navServices = getContent("doclet.navServices");
        nestedClassSummary = getContent("doclet.Nested_Class_Summary");
        newAPI = getContent("doclet.New_API");
        newLabel = getContent("doclet.New_Label");
        noScriptMessage = getContent("doclet.No_Script_Message");
        openedTo = getContent("doclet.OpenedTo");
        openModuleLabel = getContent("doclet.Open_Module");
        overridesLabel = getContent("doclet.Overrides");
        overviewLabel = getContent("doclet.Overview");
        packageHierarchies = getContent("doclet.Package_Hierarchies");
        packageLabel = getContent("doclet.Package");
        package_ = getContent("doclet.package");
        packagesLabel = getContent("doclet.Packages");
        packageSubNavLabel = getContent("doclet.Package_Sub_Nav");
        this.packageSummaryLabel = getContent("doclet.Package_Summary");
        parameters = getContent("doclet.Parameters");
        previewAPI = getContent("doclet.Preview_API");
        previewLabel = getContent("doclet.Preview_Label");
        previewMark = getContent("doclet.Preview_Mark");
        previewPhrase = getContent("doclet.Preview");
        properties = getContent("doclet.Properties");
        propertyLabel = getContent("doclet.Property");
        propertyDetailsLabel = getContent("doclet.Property_Detail");
        propertySummaryLabel = getContent("doclet.Property_Summary");
        records = getContent("doclet.RecordClasses");
        recordComponents = getContent("doclet.RecordComponents");
        referencedIn = getContent("doclet.ReferencedIn");
        relatedPackages = getContent("doclet.Related_Packages");
        returns = getContent("doclet.Returns");
        seeAlso = getContent("doclet.See_Also");
        serializedForm = getContent("doclet.Serialized_Form");
        servicesLabel = getContent("doclet.Services");
        specifiedByLabel = getContent("doclet.Specified_By");
        subclassesLabel = getContent("doclet.Subclasses");
        subinterfacesLabel = getContent("doclet.Subinterfaces");
        summaryLabel = getContent("doclet.Summary");
        systemPropertiesLabel = getContent("doclet.systemProperties");
        systemPropertiesSummaryLabel = getContent("doclet.systemPropertiesSummary");
        throws_ = getContent("doclet.Throws");
        treeLabel = getContent("doclet.Tree");
        typeLabel = getContent("doclet.Type");
        typeParameters = getContent("doclet.TypeParameters");
        useLabel = getContent("doclet.navClassUse");
        valueLabel = getContent("doclet.Value");

        navLinkLabels = new EnumMap<>(VisibleMemberTable.Kind.class);
        navLinkLabels.put(VisibleMemberTable.Kind.NESTED_CLASSES, getContent("doclet.navNested"));
        navLinkLabels.put(VisibleMemberTable.Kind.ENUM_CONSTANTS, getContent("doclet.navEnum"));
        navLinkLabels.put(VisibleMemberTable.Kind.FIELDS, getContent("doclet.navField"));
        navLinkLabels.put(VisibleMemberTable.Kind.CONSTRUCTORS, getContent("doclet.navConstructor"));
        navLinkLabels.put(VisibleMemberTable.Kind.METHODS, getContent("doclet.navMethod"));
        navLinkLabels.put(VisibleMemberTable.Kind.ANNOTATION_TYPE_MEMBER_OPTIONAL,
                getContent("doclet.navAnnotationTypeOptionalMember"));
        navLinkLabels.put(VisibleMemberTable.Kind.ANNOTATION_TYPE_MEMBER_REQUIRED,
                getContent("doclet.navAnnotationTypeRequiredMember"));
    }

    /**
     * Gets a {@code Content} object, containing the string for
     * a given key in the doclet's resources.
     *
     * @param key the key for the desired string
     * @return a content tree for the string
     */
    public Content getContent(String key) {
        return Text.of(resources.getText(key));
    }

    /**
     * Gets a {@code Content} object, containing the string for
     * a given key in the doclet's resources, formatted with
     * given arguments.
     *
     * @param key the key for the desired string
     * @param o0  string or content argument to be formatted into the result
     * @return a content tree for the text
     */
    public Content getContent(String key, Object o0) {
        return getContent(key, o0, null, null);
    }

    /**
     * Gets a {@code Content} object, containing the string for
     * a given key in the doclet's resources, formatted with
     * given arguments.

     * @param key the key for the desired string
     * @param o0  string or content argument to be formatted into the result
     * @param o1  string or content argument to be formatted into the result
     * @return a content tree for the text
     */
    public Content getContent(String key, Object o0, Object o1) {
        return getContent(key, o0, o1, null);
    }

    /**
     * Gets a {@code Content} object, containing the string for
     * a given key in the doclet's resources, formatted with
     * given arguments.
     *
     * @param key the key for the desired string
     * @param o0  string or content argument to be formatted into the result
     * @param o1  string or content argument to be formatted into the result
     * @param o2  string or content argument to be formatted into the result
     * @return a content tree for the text
     */
    public Content getContent(String key, Object o0, Object o1, Object o2) {
        Content c = new ContentBuilder();
        Pattern p = Pattern.compile("\\{([012])\\}");
        String text = resources.getText(key); // TODO: cache
        Matcher m = p.matcher(text);
        int start = 0;
        while (m.find(start)) {
            c.add(text.substring(start, m.start()));

            Object o = null;
            switch (m.group(1).charAt(0)) {
                case '0': o = o0; break;
                case '1': o = o1; break;
                case '2': o = o2; break;
            }

            if (o == null) {
                c.add("{" + m.group(1) + "}");
            } else if (o instanceof String str) {
                c.add(str);
            } else if (o instanceof Content con) {
                c.add(con);
            }

            start = m.end();
        }

        c.add(text.substring(start));
        return c;
    }

    /**
     * Returns content composed of items joined together with the specified separator.
     *
     * @param separator the separator
     * @param items     the items
     * @return the content
     */
    public Content join(Content separator, Collection<Content> items) {
        Content result = new ContentBuilder();
        boolean first = true;
        for (Content c : items) {
            if (first) {
                first = false;
            } else {
                result.add(separator);
            }
            result.add(c);
        }
        return result;
    }

    /**
     * Gets a {@code Content} object, containing the string for
     * a given key in the doclet's resources, substituting
     * <code>&nbsp;</code> for any space characters found in
     * the named resource string.
     *
     * @param key the key for the desired string
     * @return a content tree for the string
     */
    private Content getNonBreakResource(String key) {
        return getNonBreakString(resources.getText(key));
    }

    /**
     * Gets a {@code Content} object for a string, substituting
     * <code>&nbsp;</code> for any space characters found in
     * the named resource string.
     *
     * @param text the string
     * @return a content tree for the string
     */
    public Content getNonBreakString(String text) {
        Content c = new ContentBuilder();
        int start = 0;
        int p;
        while ((p = text.indexOf(" ", start)) != -1) {
            c.add(text.substring(start, p));
            c.add(Entity.NO_BREAK_SPACE);
            start = p + 1;
        }
        c.add(text.substring(start));
        return c; // TODO: should be made immutable
    }

    /**
     * Returns a content for a visible member kind.
     * @param kind the visible member table kind.
     * @return the string content
     */
    public Content getNavLinkLabelContent(VisibleMemberTable.Kind kind) {
        return Objects.requireNonNull(navLinkLabels.get(kind));
    }
}
