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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;
import jdk.javadoc.internal.doclets.toolkit.util.ClassUseMapper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;


/**
 * Generate class usage information.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassUseWriter extends SubWriterHolderWriter {

    final TypeElement typeElement;
    Set<PackageElement> pkgToPackageAnnotations = null;
    final Map<PackageElement, List<Element>> pkgToClassTypeParameter;
    final Map<PackageElement, List<Element>> pkgToClassAnnotations;
    final Map<PackageElement, List<Element>> pkgToMethodTypeParameter;
    final Map<PackageElement, List<Element>> pkgToMethodArgTypeParameter;
    final Map<PackageElement, List<Element>> pkgToMethodReturnTypeParameter;
    final Map<PackageElement, List<Element>> pkgToMethodAnnotations;
    final Map<PackageElement, List<Element>> pkgToMethodParameterAnnotations;
    final Map<PackageElement, List<Element>> pkgToFieldTypeParameter;
    final Map<PackageElement, List<Element>> pkgToFieldAnnotations;
    final Map<PackageElement, List<Element>> pkgToSubclass;
    final Map<PackageElement, List<Element>> pkgToSubinterface;
    final Map<PackageElement, List<Element>> pkgToImplementingClass;
    final Map<PackageElement, List<Element>> pkgToField;
    final Map<PackageElement, List<Element>> pkgToMethodReturn;
    final Map<PackageElement, List<Element>> pkgToMethodArgs;
    final Map<PackageElement, List<Element>> pkgToMethodThrows;
    final Map<PackageElement, List<Element>> pkgToConstructorAnnotations;
    final Map<PackageElement, List<Element>> pkgToConstructorParameterAnnotations;
    final Map<PackageElement, List<Element>> pkgToConstructorArgs;
    final Map<PackageElement, List<Element>> pkgToConstructorArgTypeParameter;
    final Map<PackageElement, List<Element>> pkgToConstructorThrows;
    final SortedSet<PackageElement> pkgSet;
    final MethodWriterImpl methodSubWriter;
    final ConstructorWriterImpl constrSubWriter;
    final FieldWriterImpl fieldSubWriter;
    final NestedClassWriterImpl classSubWriter;

    /**
     * Constructor.
     *
     * @param filename the file to be generated.
     */
    public ClassUseWriter(HtmlConfiguration configuration,
                          ClassUseMapper mapper, DocPath filename,
                          TypeElement typeElement) {
        super(configuration, filename);
        this.typeElement = typeElement;
        if (mapper.classToPackageAnnotations.containsKey(typeElement)) {
            pkgToPackageAnnotations = new TreeSet<>(comparators.makeClassUseComparator());
            pkgToPackageAnnotations.addAll(mapper.classToPackageAnnotations.get(typeElement));
        }
        configuration.currentTypeElement = typeElement;
        this.pkgSet = new TreeSet<>(comparators.makePackageComparator());
        this.pkgToClassTypeParameter = pkgDivide(mapper.classToClassTypeParam);
        this.pkgToClassAnnotations = pkgDivide(mapper.classToClassAnnotations);
        this.pkgToMethodTypeParameter = pkgDivide(mapper.classToMethodTypeParam);
        this.pkgToMethodArgTypeParameter = pkgDivide(mapper.classToMethodArgTypeParam);
        this.pkgToFieldTypeParameter = pkgDivide(mapper.classToFieldTypeParam);
        this.pkgToFieldAnnotations = pkgDivide(mapper.annotationToField);
        this.pkgToMethodReturnTypeParameter = pkgDivide(mapper.classToMethodReturnTypeParam);
        this.pkgToMethodAnnotations = pkgDivide(mapper.classToMethodAnnotations);
        this.pkgToMethodParameterAnnotations = pkgDivide(mapper.classToMethodParamAnnotation);
        this.pkgToSubclass = pkgDivide(mapper.classToSubclass);
        this.pkgToSubinterface = pkgDivide(mapper.classToSubinterface);
        this.pkgToImplementingClass = pkgDivide(mapper.classToImplementingClass);
        this.pkgToField = pkgDivide(mapper.classToField);
        this.pkgToMethodReturn = pkgDivide(mapper.classToMethodReturn);
        this.pkgToMethodArgs = pkgDivide(mapper.classToMethodArgs);
        this.pkgToMethodThrows = pkgDivide(mapper.classToMethodThrows);
        this.pkgToConstructorAnnotations = pkgDivide(mapper.classToConstructorAnnotations);
        this.pkgToConstructorParameterAnnotations = pkgDivide(mapper.classToConstructorParamAnnotation);
        this.pkgToConstructorArgs = pkgDivide(mapper.classToConstructorArgs);
        this.pkgToConstructorArgTypeParameter = pkgDivide(mapper.classToConstructorArgTypeParam);
        this.pkgToConstructorThrows = pkgDivide(mapper.classToConstructorThrows);
        //tmp test
        if (pkgSet.size() > 0 &&
            mapper.classToPackage.containsKey(this.typeElement) &&
            !pkgSet.equals(mapper.classToPackage.get(this.typeElement))) {
            configuration.reporter.print(Diagnostic.Kind.WARNING,
                    "Internal error: package sets don't match: "
                    + pkgSet + " with: " + mapper.classToPackage.get(this.typeElement));
        }

        methodSubWriter = new MethodWriterImpl(this);
        constrSubWriter = new ConstructorWriterImpl(this);
        constrSubWriter.setFoundNonPubConstructor(true);
        fieldSubWriter = new FieldWriterImpl(this);
        classSubWriter = new NestedClassWriterImpl(this);
    }

    /**
     * Write out class use pages.
     *
     * @param configuration the configuration for this doclet
     * @param classtree the class tree hierarchy
     * @throws DocFileIOException if there is an error while generating the documentation
     */
    public static void generate(HtmlConfiguration configuration, ClassTree classtree) throws DocFileIOException  {
        ClassUseMapper mapper = new ClassUseMapper(configuration, classtree);
        boolean nodeprecated = configuration.getOptions().noDeprecated();
        Utils utils = configuration.utils;
        for (TypeElement aClass : configuration.getIncludedTypeElements()) {
            // If -nodeprecated option is set and the containing package is marked
            // as deprecated, do not generate the class-use page. We will still generate
            // the class-use page if the class is marked as deprecated but the containing
            // package is not since it could still be linked from that package-use page.
            if (!(nodeprecated &&
                  utils.isDeprecated(utils.containingPackage(aClass))))
                ClassUseWriter.generate(configuration, mapper, aClass);
        }
        for (PackageElement pkg : configuration.packages) {
            // If -nodeprecated option is set and the package is marked
            // as deprecated, do not generate the package-use page.
            if (!(nodeprecated && utils.isDeprecated(pkg)))
                PackageUseWriter.generate(configuration, mapper, pkg);
        }
    }

    private Map<PackageElement, List<Element>> pkgDivide(Map<TypeElement, ? extends List<? extends Element>> classMap) {
        Map<PackageElement, List<Element>> map = new HashMap<>();
        List<? extends Element> elements = (List<? extends Element>) classMap.get(typeElement);
        if (elements != null) {
            Collections.sort(elements, comparators.makeClassUseComparator());
            for (Element e : elements) {
                PackageElement pkg = utils.containingPackage(e);
                pkgSet.add(pkg);
                List<Element> inPkg = map.get(pkg);
                if (inPkg == null) {
                    inPkg = new ArrayList<>();
                    map.put(pkg, inPkg);
                }
                inPkg.add(e);
            }
        }
        return map;
    }

    /**
     * Generate a class page.
     *
     * @throws DocFileIOException if there is a problem while generating the documentation
     */
    public static void generate(HtmlConfiguration configuration, ClassUseMapper mapper,
                                TypeElement typeElement) throws DocFileIOException {
        ClassUseWriter clsgen;
        DocPath path = configuration.docPaths.forPackage(typeElement)
                              .resolve(DocPaths.CLASS_USE)
                              .resolve(configuration.docPaths.forName( typeElement));
        clsgen = new ClassUseWriter(configuration, mapper, path, typeElement);
        clsgen.generateClassUseFile();
    }

    /**
     * Generate the class use elements.
     *
     * @throws DocFileIOException if there is a problem while generating the documentation
     */
    protected void generateClassUseFile() throws DocFileIOException {
        HtmlTree body = getClassUseHeader();
        Content mainContent = new ContentBuilder();
        if (pkgSet.size() > 0) {
            addClassUse(mainContent);
        } else {
            mainContent.add(contents.getContent("doclet.ClassUse_No.usage.of.0",
                    utils.getFullyQualifiedName(typeElement)));
        }
        bodyContents.addMainContent(mainContent);
        bodyContents.setFooter(getFooter());
        body.add(bodyContents);
        String description = getDescription("use", typeElement);
        printHtmlDocument(null, description, body);
    }

    /**
     * Add the class use documentation.
     *
     * @param contentTree the content tree to which the class use information will be added
     */
    protected void addClassUse(Content contentTree) {
        Content content = new ContentBuilder();
        if (configuration.packages.size() > 1) {
            addPackageList(content);
            addPackageAnnotationList(content);
        }
        addClassList(content);
        contentTree.add(content);
    }

    /**
     * Add the packages elements that use the given class.
     *
     * @param contentTree the content tree to which the packages elements will be added
     */
    protected void addPackageList(Content contentTree) {
        Content caption = contents.getContent(
                "doclet.ClassUse_Packages.that.use.0",
                getLink(new HtmlLinkInfo(configuration,
                        HtmlLinkInfo.Kind.CLASS_USE_HEADER, typeElement)));
        Table table = new Table(HtmlStyle.summaryTable)
                .setCaption(caption)
                .setHeader(getPackageTableHeader())
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colLast);
        for (PackageElement pkg : pkgSet) {
            addPackageUse(pkg, table);
        }
        contentTree.add(table);
    }

    /**
     * Add the package annotation elements.
     *
     * @param contentTree the content tree to which the package annotation elements will be added
     */
    protected void addPackageAnnotationList(Content contentTree) {
        if (!utils.isAnnotationType(typeElement) ||
                pkgToPackageAnnotations == null ||
                pkgToPackageAnnotations.isEmpty()) {
            return;
        }
        Content caption = contents.getContent(
                "doclet.ClassUse_PackageAnnotation",
                getLink(new HtmlLinkInfo(configuration,
                        HtmlLinkInfo.Kind.CLASS_USE_HEADER, typeElement)));

        Table table = new Table(HtmlStyle.summaryTable)
                .setCaption(caption)
                .setHeader(getPackageTableHeader())
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colLast);
        for (PackageElement pkg : pkgToPackageAnnotations) {
            Content summary = new ContentBuilder();
            addSummaryComment(pkg, summary);
            table.addRow(getPackageLink(pkg, getLocalizedPackageName(pkg)), summary);
        }
        contentTree.add(table);
    }

    /**
     * Add the class elements that use the given class.
     *
     * @param contentTree the content tree to which the class elements will be added
     */
    protected void addClassList(Content contentTree) {
        HtmlTree ul = new HtmlTree(TagName.UL);
        ul.setStyle(HtmlStyle.blockList);
        for (PackageElement pkg : pkgSet) {
            HtmlTree htmlTree = HtmlTree.SECTION(HtmlStyle.detail)
                    .setId(htmlIds.forPackage(pkg));
            Content link = contents.getContent("doclet.ClassUse_Uses.of.0.in.1",
                    getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.CLASS_USE_HEADER,
                            typeElement)),
                    getPackageLink(pkg, getLocalizedPackageName(pkg)));
            Content heading = HtmlTree.HEADING(Headings.TypeUse.SUMMARY_HEADING, link);
            htmlTree.add(heading);
            addClassUse(pkg, htmlTree);
            ul.add(HtmlTree.LI(htmlTree));
        }
        Content li = HtmlTree.SECTION(HtmlStyle.classUses, ul);
        contentTree.add(li);
    }

    /**
     * Add the package use information.
     *
     * @param pkg the package that uses the given class
     * @param table the table to which the package use information will be added
     */
    protected void addPackageUse(PackageElement pkg, Table table) {
        Content pkgLink =
                links.createLink(htmlIds.forPackage(pkg), getLocalizedPackageName(pkg));
        Content summary = new ContentBuilder();
        addSummaryComment(pkg, summary);
        table.addRow(pkgLink, summary);
    }

    /**
     * Add the class use information.
     *
     * @param pkg the package that uses the given class
     * @param contentTree the content tree to which the class use information will be added
     */
    protected void addClassUse(PackageElement pkg, Content contentTree) {
        Content classLink = getLink(new HtmlLinkInfo(configuration,
            HtmlLinkInfo.Kind.CLASS_USE_HEADER, typeElement));
        Content pkgLink = getPackageLink(pkg, getLocalizedPackageName(pkg));
        classSubWriter.addUseInfo(pkgToClassAnnotations.get(pkg),
                contents.getContent("doclet.ClassUse_Annotation", classLink,
                pkgLink), contentTree);
        classSubWriter.addUseInfo(pkgToClassTypeParameter.get(pkg),
                contents.getContent("doclet.ClassUse_TypeParameter", classLink,
                pkgLink), contentTree);
        classSubWriter.addUseInfo(pkgToSubclass.get(pkg),
                contents.getContent("doclet.ClassUse_Subclass", classLink,
                pkgLink), contentTree);
        classSubWriter.addUseInfo(pkgToSubinterface.get(pkg),
                contents.getContent("doclet.ClassUse_Subinterface", classLink,
                pkgLink), contentTree);
        classSubWriter.addUseInfo(pkgToImplementingClass.get(pkg),
                contents.getContent("doclet.ClassUse_ImplementingClass", classLink,
                pkgLink), contentTree);
        fieldSubWriter.addUseInfo(pkgToField.get(pkg),
                contents.getContent("doclet.ClassUse_Field", classLink,
                pkgLink), contentTree);
        fieldSubWriter.addUseInfo(pkgToFieldAnnotations.get(pkg),
                contents.getContent("doclet.ClassUse_FieldAnnotations", classLink,
                pkgLink), contentTree);
        fieldSubWriter.addUseInfo(pkgToFieldTypeParameter.get(pkg),
                contents.getContent("doclet.ClassUse_FieldTypeParameter", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodAnnotations.get(pkg),
                contents.getContent("doclet.ClassUse_MethodAnnotations", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodParameterAnnotations.get(pkg),
                contents.getContent("doclet.ClassUse_MethodParameterAnnotations", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodTypeParameter.get(pkg),
                contents.getContent("doclet.ClassUse_MethodTypeParameter", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodReturn.get(pkg),
                contents.getContent("doclet.ClassUse_MethodReturn", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodReturnTypeParameter.get(pkg),
                contents.getContent("doclet.ClassUse_MethodReturnTypeParameter", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodArgs.get(pkg),
                contents.getContent("doclet.ClassUse_MethodArgs", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodArgTypeParameter.get(pkg),
                contents.getContent("doclet.ClassUse_MethodArgsTypeParameters", classLink,
                pkgLink), contentTree);
        methodSubWriter.addUseInfo(pkgToMethodThrows.get(pkg),
                contents.getContent("doclet.ClassUse_MethodThrows", classLink,
                pkgLink), contentTree);
        constrSubWriter.addUseInfo(pkgToConstructorAnnotations.get(pkg),
                contents.getContent("doclet.ClassUse_ConstructorAnnotations", classLink,
                pkgLink), contentTree);
        constrSubWriter.addUseInfo(pkgToConstructorParameterAnnotations.get(pkg),
                contents.getContent("doclet.ClassUse_ConstructorParameterAnnotations", classLink,
                pkgLink), contentTree);
        constrSubWriter.addUseInfo(pkgToConstructorArgs.get(pkg),
                contents.getContent("doclet.ClassUse_ConstructorArgs", classLink,
                pkgLink), contentTree);
        constrSubWriter.addUseInfo(pkgToConstructorArgTypeParameter.get(pkg),
                contents.getContent("doclet.ClassUse_ConstructorArgsTypeParameters", classLink,
                pkgLink), contentTree);
        constrSubWriter.addUseInfo(pkgToConstructorThrows.get(pkg),
                contents.getContent("doclet.ClassUse_ConstructorThrows", classLink,
                pkgLink), contentTree);
    }

    /**
     * Get the header for the class use Listing.
     *
     * @return a content tree representing the class use header
     */
    protected HtmlTree getClassUseHeader() {
        String cltype = resources.getText(switch (typeElement.getKind()) {
            case ANNOTATION_TYPE -> "doclet.AnnotationType";
            case INTERFACE -> "doclet.Interface";
            case RECORD -> "doclet.RecordClass";
            case ENUM -> "doclet.Enum";
            default -> "doclet.Class";
        });
        String clname = utils.getFullyQualifiedName(typeElement);
        String title = resources.getText("doclet.Window_ClassUse_Header",
                cltype, clname);
        HtmlTree bodyTree = getBody(getWindowTitle(title));
        ContentBuilder headingContent = new ContentBuilder();
        headingContent.add(contents.getContent("doclet.ClassUse_Title", cltype));
        headingContent.add(new HtmlTree(TagName.BR));
        headingContent.add(clname);
        Content heading = HtmlTree.HEADING_TITLE(Headings.PAGE_TITLE_HEADING,
                HtmlStyle.title, headingContent);
        Content div = HtmlTree.DIV(HtmlStyle.header, heading);
        bodyContents.setHeader(getHeader(PageMode.USE, typeElement)).addMainContent(div);
        return bodyTree;
    }

    @Override
    protected Navigation getNavBar(PageMode pageMode, Element element) {
        Content mdleLinkContent = getModuleLink(utils.elementUtils.getModuleOf(typeElement),
                contents.moduleLabel);
        Content classLinkContent = getLink(new HtmlLinkInfo(
                configuration, HtmlLinkInfo.Kind.CLASS_USE_HEADER, typeElement)
                .label(resources.getText("doclet.Class"))
                .skipPreview(true));
        return super.getNavBar(pageMode, element)
                .setNavLinkModule(mdleLinkContent)
                .setNavLinkClass(classLinkContent);
    }
}
