/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.builders;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.toolkit.ClassWriter;
import jdk.javadoc.internal.doclets.toolkit.CommentUtils;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocFilesHandler;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * Builds the summary for a given class.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassBuilder extends AbstractBuilder {

    /**
     * The class being documented.
     */
    private final TypeElement typeElement;

    /**
     * The doclet specific writer.
     */
    private final ClassWriter writer;

    /**
     * The content tree for the class documentation.
     */
    private Content contentTree;

    private final Utils utils;

    /**
     * Construct a new ClassBuilder.
     *
     * @param context  the build context
     * @param typeElement the class being documented.
     * @param writer the doclet specific writer.
     */
    private ClassBuilder(Context context, TypeElement typeElement, ClassWriter writer) {
        super(context);
        this.typeElement = typeElement;
        this.writer = writer;
        this.utils = configuration.utils;
        switch (typeElement.getKind()) {
            case ENUM:
                setEnumDocumentation(typeElement);
                break;

            case RECORD:
                setRecordDocumentation(typeElement);
                break;
        }
    }

    /**
     * Constructs a new ClassBuilder.
     *
     * @param context  the build context
     * @param typeElement the class being documented.
     * @param writer the doclet specific writer.
     * @return the new ClassBuilder
     */
    public static ClassBuilder getInstance(Context context, TypeElement typeElement, ClassWriter writer) {
        return new ClassBuilder(context, typeElement, writer);
    }

    @Override
    public void build() throws DocletException {
        buildClassDoc();
    }

     /**
      * Handles the {@literal <TypeElement>} tag.
      *
      * @throws DocletException if there is a problem while building the documentation
      */
     protected void buildClassDoc() throws DocletException {
        String key;
         switch (typeElement.getKind()) {
             case INTERFACE:
                 key = "doclet.Interface";
                 break;
             case ENUM:
                 key = "doclet.Enum";
                 break;
             case RECORD:
                 key = "doclet.RecordClass";
                 break;
             case ANNOTATION_TYPE:
                 key = "doclet.AnnotationType";
                 break;
             case CLASS:
                 key = "doclet.Class";
                 break;
             default:
                 throw new IllegalStateException(typeElement.getKind() + " " + typeElement);
         }
        Content contentTree = writer.getHeader(resources.getText(key) + " "
                + utils.getSimpleName(typeElement));
        Content classContentTree = writer.getClassContentHeader();

        buildClassTree(classContentTree);
        buildClassInfo(classContentTree);
        buildMemberSummary(classContentTree);
        buildMemberDetails(classContentTree);

        writer.addClassContentTree(classContentTree);
        writer.addFooter();
        writer.printDocument(contentTree);
        copyDocFiles();
    }

     /**
      * Build the class tree documentation.
      *
      * @param classContentTree the content tree to which the documentation will be added
      */
    protected void buildClassTree(Content classContentTree) {
        writer.addClassTree(classContentTree);
    }

    /**
     * Build the class information tree documentation.
     *
     * @param classContentTree the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildClassInfo(Content classContentTree) throws DocletException {
        Content classInfoTree = new ContentBuilder();
        buildParamInfo(classInfoTree);
        buildSuperInterfacesInfo(classInfoTree);
        buildImplementedInterfacesInfo(classInfoTree);
        buildSubClassInfo(classInfoTree);
        buildSubInterfacesInfo(classInfoTree);
        buildInterfaceUsageInfo(classInfoTree);
        buildNestedClassInfo(classInfoTree);
        buildFunctionalInterfaceInfo(classInfoTree);
        buildClassSignature(classInfoTree);
        buildDeprecationInfo(classInfoTree);
        buildClassDescription(classInfoTree);
        buildClassTagInfo(classInfoTree);

        classContentTree.add(writer.getClassInfo(classInfoTree));
    }

    /**
     * Build the type parameters and state components of this class.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildParamInfo(Content classInfoTree) {
        writer.addParamInfo(classInfoTree);
    }

    /**
     * If this is an interface, list all super interfaces.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildSuperInterfacesInfo(Content classInfoTree) {
        writer.addSuperInterfacesInfo(classInfoTree);
    }

    /**
     * If this is a class, list all interfaces implemented by this class.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildImplementedInterfacesInfo(Content classInfoTree) {
        writer.addImplementedInterfacesInfo(classInfoTree);
    }

    /**
     * List all the classes extend this one.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildSubClassInfo(Content classInfoTree) {
        writer.addSubClassInfo(classInfoTree);
    }

    /**
     * List all the interfaces that extend this one.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildSubInterfacesInfo(Content classInfoTree) {
        writer.addSubInterfacesInfo(classInfoTree);
    }

    /**
     * If this is an interface, list all classes that implement this interface.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildInterfaceUsageInfo(Content classInfoTree) {
        writer.addInterfaceUsageInfo(classInfoTree);
    }

    /**
     * If this is an functional interface, display appropriate message.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildFunctionalInterfaceInfo(Content classInfoTree) {
        writer.addFunctionalInterfaceInfo(classInfoTree);
    }

    /**
     * If this class is deprecated, build the appropriate information.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content classInfoTree) {
        writer.addClassDeprecationInfo(classInfoTree);
    }

    /**
     * If this is an inner class or interface, list the enclosing class or interface.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildNestedClassInfo(Content classInfoTree) {
        writer.addNestedClassInfo(classInfoTree);
    }

    /**
     * Copy the doc files.
     *
     * @throws DocFileIOException if there is a problem while copying the files
     */
     private void copyDocFiles() throws DocletException {
        PackageElement containingPackage = utils.containingPackage(typeElement);
        if ((configuration.packages == null ||
            !configuration.packages.contains(containingPackage)) &&
            !containingPackagesSeen.contains(containingPackage)) {
            //Only copy doc files dir if the containing package is not
            //documented AND if we have not documented a class from the same
            //package already. Otherwise, we are making duplicate copies.
            DocFilesHandler docFilesHandler = configuration
                    .getWriterFactory()
                    .getDocFilesHandler(containingPackage);
            docFilesHandler.copyDocFiles();
            containingPackagesSeen.add(containingPackage);
        }
     }

    /**
     * Build the signature of the current class.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildClassSignature(Content classInfoTree) {
        writer.addClassSignature(classInfoTree);
    }

    /**
     * Build the class description.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildClassDescription(Content classInfoTree) {
       writer.addClassDescription(classInfoTree);
    }

    /**
     * Build the tag information for the current class.
     *
     * @param classInfoTree the content tree to which the documentation will be added
     */
    protected void buildClassTagInfo(Content classInfoTree) {
       writer.addClassTagInfo(classInfoTree);
    }

    /**
     * Build the member summary contents of the page.
     *
     * @param classContentTree the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMemberSummary(Content classContentTree) throws DocletException {
        Content summariesList = writer.getSummariesList();
        builderFactory.getMemberSummaryBuilder(writer).build(summariesList);
        classContentTree.add(writer.getMemberSummaryTree(summariesList));
    }

    /**
     * Build the member details contents of the page.
     *
     * @param classContentTree the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMemberDetails(Content classContentTree) throws DocletException {
        Content detailsList = writer.getDetailsList();

        buildEnumConstantsDetails(detailsList);
        buildPropertyDetails(detailsList);
        buildFieldDetails(detailsList);
        buildConstructorDetails(detailsList);
        buildAnnotationTypeRequiredMemberDetails(detailsList);
        buildAnnotationTypeOptionalMemberDetails(detailsList);
        buildMethodDetails(detailsList);

        classContentTree.add(writer.getMemberDetailsTree(detailsList));
    }

    /**
     * Build the enum constants documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildEnumConstantsDetails(Content detailsList) throws DocletException {
        builderFactory.getEnumConstantsBuilder(writer).build(detailsList);
    }

    /**
     * Build the field documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildFieldDetails(Content detailsList) throws DocletException {
        builderFactory.getFieldBuilder(writer).build(detailsList);
    }

    /**
     * Build the property documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    public void buildPropertyDetails( Content detailsList) throws DocletException {
        builderFactory.getPropertyBuilder(writer).build(detailsList);
    }

    /**
     * Build the constructor documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildConstructorDetails(Content detailsList) throws DocletException {
        builderFactory.getConstructorBuilder(writer).build(detailsList);
    }

    /**
     * Build the method documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMethodDetails(Content detailsList) throws DocletException {
        builderFactory.getMethodBuilder(writer).build(detailsList);
    }

    /**
     * Build the annotation type optional member documentation.
     *
     * @param memberDetailsTree the content tree to which the documentation will be added
     * @throws DocletException if there is a problem building the documentation
     */
    protected void buildAnnotationTypeOptionalMemberDetails(Content memberDetailsTree)
            throws DocletException {
        builderFactory.getAnnotationTypeOptionalMemberBuilder(writer).build(memberDetailsTree);
    }

    /**
     * Build the annotation type required member documentation.
     *
     * @param memberDetailsTree the content tree to which the documentation will be added
     * @throws DocletException if there is a problem building the documentation
     */
    protected void buildAnnotationTypeRequiredMemberDetails(Content memberDetailsTree)
            throws DocletException {
        builderFactory.getAnnotationTypeRequiredMemberBuilder(writer).build(memberDetailsTree);
    }

    /**
     * The documentation for values() and valueOf() in Enums are set by the
     * doclet only iff the user or overridden methods are missing.
     * @param elem the enum element
     */
    private void setEnumDocumentation(TypeElement elem) {
        CommentUtils cmtUtils = configuration.cmtUtils;
        for (ExecutableElement ee : utils.getMethods(elem)) {
            if (!utils.getFullBody(ee).isEmpty()) // ignore if already set
                continue;
            Name name = ee.getSimpleName();
            if (name.contentEquals("values") && ee.getParameters().isEmpty()) {
                utils.removeCommentHelper(ee); // purge previous entry
                cmtUtils.setEnumValuesTree(ee);
            } else if (name.contentEquals("valueOf") && ee.getParameters().size() == 1) {
                // TODO: check parameter type
                utils.removeCommentHelper(ee); // purge previous entry
                cmtUtils.setEnumValueOfTree(ee);
            }
        }
    }

    /**
     * Sets the documentation as needed for the mandated parts of a record type.
     * This includes the canonical constructor, methods like {@code equals},
     * {@code hashCode}, {@code toString}, the accessor methods, and the underlying
     * field.
     * @param elem the record element
     */

    private void setRecordDocumentation(TypeElement elem) {
        CommentUtils cmtUtils = configuration.cmtUtils;
        Set<Name> componentNames = elem.getRecordComponents().stream()
                .map(Element::getSimpleName)
                .collect(Collectors.toSet());

        for (ExecutableElement ee : utils.getConstructors(elem)) {
            if (utils.isCanonicalRecordConstructor(ee)) {
                if (utils.getFullBody(ee).isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordConstructorTree(ee);
                }
                // only one canonical constructor; no need to keep looking
                break;
            }
        }

        for (VariableElement ve : utils.getFields(elem)) {
            // The fields for the record component cannot be declared by the
            // user and so cannot have any pre-existing comment.
            Name name = ve.getSimpleName();
            if (componentNames.contains(name)) {
                utils.removeCommentHelper(ve); // purge previous entry
                cmtUtils.setRecordFieldTree(ve);
            }
        }

        TypeMirror objectType = utils.getObjectType();

        for (ExecutableElement ee : utils.getMethods(elem)) {
            if (!utils.getFullBody(ee).isEmpty()) {
                continue;
            }

            Name name = ee.getSimpleName();
            List<? extends VariableElement> params = ee.getParameters();
            if (name.contentEquals("equals")) {
                if (params.size() == 1 && utils.typeUtils.isSameType(params.get(0).asType(), objectType)) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordEqualsTree(ee);
                }
            } else if (name.contentEquals("hashCode")) {
                if (params.isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordHashCodeTree(ee);
                }
            } else if (name.contentEquals("toString")) {
                if (params.isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordToStringTree(ee);
                }
            } else if (componentNames.contains(name)) {
                if (params.isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordAccessorTree(ee);
                }
            }
        }

    }
}
