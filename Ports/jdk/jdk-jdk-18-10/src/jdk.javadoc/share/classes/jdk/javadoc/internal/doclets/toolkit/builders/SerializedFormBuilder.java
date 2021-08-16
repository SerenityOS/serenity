/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.SortedSet;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.SerializedFormWriter;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * Builds the serialized form.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SerializedFormBuilder extends AbstractBuilder {

    /**
     * The writer for this builder.
     */
    private SerializedFormWriter writer;

    /**
     * The writer for serializable fields.
     */
    private SerializedFormWriter.SerialFieldWriter fieldWriter;

    /**
     * The writer for serializable method documentation.
     */
    private SerializedFormWriter.SerialMethodWriter methodWriter;

    /**
     * The header for the serial version UID.  Save the string
     * here instead of the properties file because we do not want
     * this string to be localized.
     */
    private static final String SERIAL_VERSION_UID = "serialVersionUID";
    private static final String SERIAL_VERSION_UID_HEADER = SERIAL_VERSION_UID + ":";

    /**
     * The current package being documented.
     */
    private PackageElement currentPackage;

    /**
     * The current class being documented.
     */
    private TypeElement currentTypeElement;

    /**
     * The current member being documented.
     */
    protected Element currentMember;

    /**
     * Construct a new SerializedFormBuilder.
     * @param context  the build context.
     */
    private SerializedFormBuilder(Context context) {
        super(context);
    }

    /**
     * Construct a new SerializedFormBuilder.
     *
     * @param context  the build context.
     * @return the new SerializedFormBuilder
     */
    public static SerializedFormBuilder getInstance(Context context) {
        return new SerializedFormBuilder(context);
    }

    /**
     * Build the serialized form.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    @Override
    public void build() throws DocletException {
        SortedSet<TypeElement> rootclasses = new TreeSet<>(utils.comparators.makeGeneralPurposeComparator());
        rootclasses.addAll(configuration.getIncludedTypeElements());
        if (!serialClassFoundToDocument(rootclasses)) {
            //Nothing to document.
            return;
        }
        writer = configuration.getWriterFactory().getSerializedFormWriter();
        if (writer == null) {
            //Doclet does not support this output.
            return;
        }
        buildSerializedForm();
    }

    /**
     * Build the serialized form.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildSerializedForm() throws DocletException {
        Content contentTree = writer.getHeader(resources.getText(
                "doclet.Serialized_Form"));

        buildSerializedFormSummaries();

        writer.addFooter();
        writer.printDocument(contentTree);
    }

    /**
     * Build the serialized form summaries.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildSerializedFormSummaries()
            throws DocletException {
        Content serializedSummariesTree = writer.getSerializedSummariesHeader();
        for (PackageElement pkg : configuration.packages) {
            currentPackage = pkg;

            buildPackageSerializedForm(serializedSummariesTree);
        }
        writer.addSerializedContent(serializedSummariesTree);
    }

    /**
     * Build the package serialized form for the current package being processed.
     *
     * @param serializedSummariesTree content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildPackageSerializedForm(Content serializedSummariesTree) throws DocletException {
        Content packageSerializedTree = writer.getPackageSerializedHeader();
        SortedSet<TypeElement> classes = utils.getAllClassesUnfiltered(currentPackage);
        if (classes.isEmpty()) {
            return;
        }
        if (!serialInclude(utils, currentPackage)) {
            return;
        }
        if (!serialClassFoundToDocument(classes)) {
            return;
        }

        buildPackageHeader(packageSerializedTree);
        buildClassSerializedForm(packageSerializedTree);

        writer.addPackageSerializedTree(serializedSummariesTree, packageSerializedTree);
    }

    /**
     * Build the package header.
     *
     * @param packageSerializedTree content tree to which the documentation will be added
     */
    protected void buildPackageHeader(Content packageSerializedTree) {
        packageSerializedTree.add(writer.getPackageHeader(currentPackage));
    }

    /**
     * Build the class serialized form.
     *
     * @param packageSerializedTree content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildClassSerializedForm(Content packageSerializedTree)
            throws DocletException {
        Content classSerializedTree = writer.getClassSerializedHeader();
        SortedSet<TypeElement> typeElements = utils.getAllClassesUnfiltered(currentPackage);
        for (TypeElement typeElement : typeElements) {
            currentTypeElement = typeElement;
            fieldWriter = writer.getSerialFieldWriter(currentTypeElement);
            methodWriter = writer.getSerialMethodWriter(currentTypeElement);
            if (utils.isClass(currentTypeElement) && utils.isSerializable(currentTypeElement)) {
                if (!serialClassInclude(utils, currentTypeElement)) {
                    continue;
                }
                Content classTree = writer.getClassHeader(currentTypeElement);

                buildSerialUIDInfo(classTree);
                buildClassContent(classTree);

                classSerializedTree.add(writer.getMemberTree(classTree));
            }
        }
        packageSerializedTree.add(classSerializedTree);
    }

    /**
     * Build the serial UID information for the given class.
     *
     * @param classTree content tree to which the serial UID information will be added
     */
    protected void buildSerialUIDInfo(Content classTree) {
        Content serialUidTree = writer.getSerialUIDInfoHeader();
        for (Element e : utils.getFieldsUnfiltered(currentTypeElement)) {
            VariableElement field = (VariableElement)e;
            if (field.getSimpleName().toString().compareTo(SERIAL_VERSION_UID) == 0 &&
                field.getConstantValue() != null) {
                writer.addSerialUIDInfo(SERIAL_VERSION_UID_HEADER,
                                        utils.constantValueExpression(field), serialUidTree);
                break;
            }
        }
        classTree.add(serialUidTree);
    }

    /**
     * Build the summaries for the methods and fields.
     *
     * @param classTree content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildClassContent(Content classTree) throws DocletException {
        Content classContentTree = writer.getClassContentHeader();

        buildSerializableMethods(classContentTree);
        buildFieldHeader(classContentTree);
        buildSerializableFields(classContentTree);

        classTree.add(classContentTree);
    }

    /**
     * Build the summaries for the methods that belong to the given class.
     *
     * @param classContentTree content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildSerializableMethods(Content classContentTree) throws DocletException {
        Content serializableMethodTree = methodWriter.getSerializableMethodsHeader();
        SortedSet<ExecutableElement> members = utils.serializationMethods(currentTypeElement);
        if (!members.isEmpty()) {
            for (ExecutableElement member : members) {
                currentMember = member;
                Content methodsContentTree = methodWriter.getMethodsContentHeader(
                        currentMember == members.last());

                buildMethodSubHeader(methodsContentTree);
                buildDeprecatedMethodInfo(methodsContentTree);
                buildMethodInfo(methodsContentTree);

                serializableMethodTree.add(methodsContentTree);
            }
        }
        if (!utils.serializationMethods(currentTypeElement).isEmpty()) {
            classContentTree.add(methodWriter.getSerializableMethods(
                    resources.getText("doclet.Serialized_Form_methods"),
                    serializableMethodTree));
            if (utils.isSerializable(currentTypeElement) && !utils.isExternalizable(currentTypeElement)) {
                if (utils.serializationMethods(currentTypeElement).isEmpty()) {
                    Content noCustomizationMsg = methodWriter.getNoCustomizationMsg(
                            resources.getText("doclet.Serializable_no_customization"));
                    classContentTree.add(methodWriter.getSerializableMethods(
                            resources.getText("doclet.Serialized_Form_methods"),
                            noCustomizationMsg));
                }
            }
        }
    }

    /**
     * Build the method sub header.
     *
     * @param methodsContentTree content tree to which the documentation will be added
     */
    protected void buildMethodSubHeader(Content methodsContentTree)  {
        methodWriter.addMemberHeader((ExecutableElement)currentMember, methodsContentTree);
    }

    /**
     * Build the deprecated method description.
     *
     * @param methodsContentTree content tree to which the documentation will be added
     */
    protected void buildDeprecatedMethodInfo(Content methodsContentTree) {
        methodWriter.addDeprecatedMemberInfo((ExecutableElement)currentMember, methodsContentTree);
    }

    /**
     * Build the information for the method.
     *
     * @param methodsContentTree content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMethodInfo(Content methodsContentTree) throws DocletException  {
        if (options.noComment()) {
            return;
        }

        buildMethodDescription(methodsContentTree);
        buildMethodTags(methodsContentTree);
    }

    /**
     * Build method description.
     *
     * @param methodsContentTree content tree to which the documentation will be added
     */
    protected void buildMethodDescription(Content methodsContentTree) {
        methodWriter.addMemberDescription((ExecutableElement)currentMember, methodsContentTree);
    }

    /**
     * Build the method tags.
     *
     * @param methodsContentTree content tree to which the documentation will be added
     */
    protected void buildMethodTags(Content methodsContentTree) {
        methodWriter.addMemberTags((ExecutableElement)currentMember, methodsContentTree);
        ExecutableElement method = (ExecutableElement)currentMember;
        if (method.getSimpleName().toString().compareTo("writeExternal") == 0
                && utils.getSerialDataTrees(method).isEmpty()) {
            if (options.serialWarn()) {
                TypeElement encl  = (TypeElement) method.getEnclosingElement();
                messages.warning(currentMember,
                        "doclet.MissingSerialDataTag", encl.getQualifiedName().toString(),
                        method.getSimpleName().toString());
            }
        }
    }

    /**
     * Build the field header.
     *
     * @param classContentTree content tree to which the documentation will be added
     */
    protected void buildFieldHeader(Content classContentTree) {
        if (!utils.serializableFields(currentTypeElement).isEmpty()) {
            buildFieldSerializationOverview(currentTypeElement, classContentTree);
        }
    }

    /**
     * Build the serialization overview for the given class.
     *
     * @param typeElement the class to print the overview for.
     * @param classContentTree content tree to which the documentation will be added
     */
    public void buildFieldSerializationOverview(TypeElement typeElement, Content classContentTree) {
        if (utils.definesSerializableFields(typeElement)) {
            VariableElement ve = utils.serializableFields(typeElement).first();
            // Check to see if there are inline comments, tags or deprecation
            // information to be printed.
            if (fieldWriter.shouldPrintOverview(ve)) {
                Content serializableFieldsTree = fieldWriter.getSerializableFieldsHeader();
                Content fieldsOverviewContentTree = fieldWriter.getFieldsContentHeader(true);
                fieldWriter.addMemberDeprecatedInfo(ve, fieldsOverviewContentTree);
                if (!options.noComment()) {
                    fieldWriter.addMemberDescription(ve, fieldsOverviewContentTree);
                    fieldWriter.addMemberTags(ve, fieldsOverviewContentTree);
                }
                serializableFieldsTree.add(fieldsOverviewContentTree);
                classContentTree.add(fieldWriter.getSerializableFields(
                        resources.getText("doclet.Serialized_Form_class"),
                        serializableFieldsTree));
            }
        }
    }

    /**
     * Build the summaries for the fields that belong to the given class.
     *
     * @param classContentTree content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildSerializableFields(Content classContentTree)
            throws DocletException {
        SortedSet<VariableElement> members = utils.serializableFields(currentTypeElement);
        if (!members.isEmpty()) {
            Content serializableFieldsTree = fieldWriter.getSerializableFieldsHeader();
            for (VariableElement ve : members) {
                currentMember = ve;
                if (!utils.definesSerializableFields(currentTypeElement)) {
                    Content fieldsContentTree = fieldWriter.getFieldsContentHeader(
                            currentMember == members.last());

                    buildFieldSubHeader(fieldsContentTree);
                    buildFieldDeprecationInfo(fieldsContentTree);
                    buildFieldInfo(fieldsContentTree);

                    serializableFieldsTree.add(fieldsContentTree);
                } else {
                    buildSerialFieldTagsInfo(serializableFieldsTree);
                }
            }
            classContentTree.add(fieldWriter.getSerializableFields(
                    resources.getText("doclet.Serialized_Form_fields"),
                    serializableFieldsTree));
        }
    }

    /**
     * Build the field sub header.
     *
     * @param fieldsContentTree content tree to which the documentation will be added
     */
    protected void buildFieldSubHeader(Content fieldsContentTree) {
        if (!utils.definesSerializableFields(currentTypeElement)) {
            VariableElement field = (VariableElement) currentMember;
            fieldWriter.addMemberHeader(field.asType(),
                    utils.getSimpleName(field),
                    fieldsContentTree);
        }
    }

    /**
     * Build the field deprecation information.
     *
     * @param fieldsContentTree content tree to which the documentation will be added
     */
    protected void buildFieldDeprecationInfo(Content fieldsContentTree) {
        if (!utils.definesSerializableFields(currentTypeElement)) {
            fieldWriter.addMemberDeprecatedInfo((VariableElement)currentMember,
                    fieldsContentTree);
        }
    }

    /**
     * Build the serial field tags information.
     *
     * @param serializableFieldsTree content tree to which the documentation will be added
     */
    protected void buildSerialFieldTagsInfo(Content serializableFieldsTree) {
        if (options.noComment()) {
            return;
        }
        VariableElement field = (VariableElement)currentMember;
        // Process Serializable Fields specified as array of
        // ObjectStreamFields. Print a member for each serialField tag.
        // (There should be one serialField tag per ObjectStreamField
        // element.)
        SortedSet<SerialFieldTree> tags = new TreeSet<>(utils.comparators.makeSerialFieldTreeComparator());
        // sort the elements
        tags.addAll(utils.getSerialFieldTrees(field));

        CommentHelper ch = utils.getCommentHelper(field);
        for (SerialFieldTree tag : tags) {
            if (tag.getName() == null || tag.getType() == null)  // ignore malformed @serialField tags
                continue;
            Content fieldsContentTree = fieldWriter.getFieldsContentHeader(tag.equals(tags.last()));
            TypeMirror type = ch.getReferencedType(tag);
            fieldWriter.addMemberHeader(type, tag.getName().getName().toString(), fieldsContentTree);
            fieldWriter.addMemberDescription(field, tag, fieldsContentTree);
            serializableFieldsTree.add(fieldsContentTree);
        }
    }

    /**
     * Build the field information.
     *
     * @param fieldsContentTree content tree to which the documentation will be added
     */
    protected void buildFieldInfo(Content fieldsContentTree) {
        if (options.noComment()) {
            return;
        }
        VariableElement field = (VariableElement)currentMember;
        TypeElement te = utils.getEnclosingTypeElement(currentMember);
        // Process default Serializable field.
        if ((utils.getSerialTrees(field).isEmpty()) /*&& !field.isSynthetic()*/
                && options.serialWarn()) {
            messages.warning(field,
                    "doclet.MissingSerialTag", utils.getFullyQualifiedName(te),
                    utils.getSimpleName(field));
        }
        fieldWriter.addMemberDescription(field, fieldsContentTree);
        fieldWriter.addMemberTags(field, fieldsContentTree);
    }

    /**
     * Returns true if the given Element should be included
     * in the serialized form.
     *
     * @param utils the utils object
     * @param element the Element object to check for serializability
     * @return true if the element should be included in the serial form
     */
    public static boolean serialInclude(Utils utils, Element element) {
        if (element == null) {
            return false;
        }
        return utils.isClass(element)
                ? serialClassInclude(utils, (TypeElement)element)
                : serialDocInclude(utils, element);
    }

    /**
     * Returns true if the given TypeElement should be included
     * in the serialized form.
     *
     * @param te the TypeElement object to check for serializability.
     */
    private static boolean serialClassInclude(Utils utils, TypeElement te) {
        if (utils.isEnum(te)) {
            return false;
        }
        if (utils.isSerializable(te)) {
            if (utils.hasDocCommentTree(te) && !utils.getSerialTrees(te).isEmpty()) {
                return serialDocInclude(utils, te);
            } else {
                return utils.isPublic(te) || utils.isProtected(te);
            }
        }
        return false;
    }

    /**
     * Return true if the given Element should be included
     * in the serialized form.
     *
     * @param element the Element to check for serializability.
     */
    private static boolean serialDocInclude(Utils utils, Element element) {
        if (utils.isEnum(element)) {
            return false;
        }
        List<? extends SerialTree> serial = utils.getSerialTrees(element);
        if (!serial.isEmpty()) {
            CommentHelper ch = utils.getCommentHelper(element);
            String serialtext = Utils.toLowerCase(ch.getText(serial.get(0)));
            if (serialtext.contains("exclude")) {
                return false;
            } else if (serialtext.contains("include")) {
                return true;
            }
        }
        return true;
    }

    /**
     * Return true if any of the given typeElements have a {@code @serial include} tag.
     *
     * @param classes the typeElements to check.
     * @return true if any of the given typeElements have a {@code @serial include} tag.
     */
    private boolean serialClassFoundToDocument(SortedSet<TypeElement> classes) {
        for (TypeElement aClass : classes) {
            if (serialClassInclude(utils, aClass)) {
                return true;
            }
        }
        return false;
    }
}
