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

package jdk.javadoc.internal.doclets.formats.html;


import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.AnnotationTypeOptionalMemberWriter;
import jdk.javadoc.internal.doclets.toolkit.AnnotationTypeRequiredMemberWriter;
import jdk.javadoc.internal.doclets.toolkit.ClassWriter;
import jdk.javadoc.internal.doclets.toolkit.ConstantsSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.DocFilesHandler;
import jdk.javadoc.internal.doclets.toolkit.MemberSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.ModuleSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.PackageSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.SerializedFormWriter;
import jdk.javadoc.internal.doclets.toolkit.WriterFactory;
import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;

/**
 * The factory that returns HTML writers.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class WriterFactoryImpl implements WriterFactory {

    private final HtmlConfiguration configuration;
    public WriterFactoryImpl(HtmlConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ConstantsSummaryWriter getConstantsSummaryWriter() {
        return new ConstantsSummaryWriterImpl(configuration);
    }

    @Override
    public PackageSummaryWriter getPackageSummaryWriter(PackageElement packageElement) {
        return new PackageWriterImpl(configuration, packageElement);
    }

    @Override
    public ModuleSummaryWriter getModuleSummaryWriter(ModuleElement mdle) {
        return new ModuleWriterImpl(configuration, mdle);
    }

    @Override
    public ClassWriter getClassWriter(TypeElement typeElement, ClassTree classTree) {
        return new ClassWriterImpl(configuration, typeElement, classTree);
    }

    @Override
    public AnnotationTypeOptionalMemberWriter getAnnotationTypeOptionalMemberWriter(
            ClassWriter classWriter) {
        TypeElement te = classWriter.getTypeElement();
        return new AnnotationTypeOptionalMemberWriterImpl(
                (ClassWriterImpl) classWriter, te);
    }

    @Override
    public AnnotationTypeRequiredMemberWriter getAnnotationTypeRequiredMemberWriter(
            ClassWriter classWriter) {
        TypeElement te = classWriter.getTypeElement();
        return new AnnotationTypeRequiredMemberWriterImpl(
            (ClassWriterImpl) classWriter, te);
    }

    @Override
    public EnumConstantWriterImpl getEnumConstantWriter(ClassWriter classWriter) {
        return new EnumConstantWriterImpl((ClassWriterImpl) classWriter,
                classWriter.getTypeElement());
    }

    @Override
    public FieldWriterImpl getFieldWriter(ClassWriter classWriter) {
        return new FieldWriterImpl((ClassWriterImpl) classWriter, classWriter.getTypeElement());
    }

    @Override
    public PropertyWriterImpl getPropertyWriter(ClassWriter classWriter) {
        return new PropertyWriterImpl((ClassWriterImpl) classWriter,
                classWriter.getTypeElement());
    }

    @Override
    public MethodWriterImpl getMethodWriter(ClassWriter classWriter) {
        return new MethodWriterImpl((ClassWriterImpl) classWriter, classWriter.getTypeElement());
    }

    @Override
    public ConstructorWriterImpl getConstructorWriter(ClassWriter classWriter) {
        return new ConstructorWriterImpl((ClassWriterImpl) classWriter,
                classWriter.getTypeElement());
    }

    @Override
    public MemberSummaryWriter getMemberSummaryWriter(ClassWriter classWriter,
            VisibleMemberTable.Kind memberType) {
        switch (memberType) {
            case CONSTRUCTORS:
                return getConstructorWriter(classWriter);
            case ENUM_CONSTANTS:
                return getEnumConstantWriter(classWriter);
            case ANNOTATION_TYPE_MEMBER_OPTIONAL:
                return (AnnotationTypeOptionalMemberWriterImpl)
                        getAnnotationTypeOptionalMemberWriter(classWriter);
            case ANNOTATION_TYPE_MEMBER_REQUIRED:
                return (AnnotationTypeRequiredMemberWriterImpl)
                        getAnnotationTypeRequiredMemberWriter(classWriter);
            case FIELDS:
                return getFieldWriter(classWriter);
            case PROPERTIES:
                return getPropertyWriter(classWriter);
            case NESTED_CLASSES:
                return new NestedClassWriterImpl((SubWriterHolderWriter)
                    classWriter, classWriter.getTypeElement());
            case METHODS:
                return getMethodWriter(classWriter);
            default:
                return null;
        }
    }

    @Override
    public SerializedFormWriter getSerializedFormWriter() {
        return new SerializedFormWriterImpl(configuration);
    }

    @Override
    public DocFilesHandler getDocFilesHandler(Element element) {
        return new DocFilesHandlerImpl(configuration, element);
    }
}
