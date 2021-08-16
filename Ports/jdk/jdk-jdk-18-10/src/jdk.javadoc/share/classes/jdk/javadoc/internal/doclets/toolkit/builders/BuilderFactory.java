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

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.ClassWriter;
import jdk.javadoc.internal.doclets.toolkit.PropertyWriter;
import jdk.javadoc.internal.doclets.toolkit.WriterFactory;
import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;




/**
 * The factory for constructing builders.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public class BuilderFactory {

    /**
     * The factory to retrieve the required writers from.
     */
    private final WriterFactory writerFactory;

    private final AbstractBuilder.Context context;

    /**
     * Construct a builder factory using the given configuration.
     * @param configuration the configuration for the current doclet
     * being executed.
     */
    public BuilderFactory (BaseConfiguration configuration) {
        this.writerFactory = configuration.getWriterFactory();

        Set<PackageElement> containingPackagesSeen = new HashSet<>();
        context = new AbstractBuilder.Context(configuration, containingPackagesSeen);
    }

    /**
     * Return the builder that builds the constant summary.
     * @return the builder that builds the constant summary.
     */
    public AbstractBuilder getConstantsSummaryBuilder() {
        return ConstantsSummaryBuilder.getInstance(context);
    }

    /**
     * Return the builder that builds the package summary.
     *
     * @param pkg the package being documented.
     * @return the builder that builds the package summary.
     */
    public AbstractBuilder getPackageSummaryBuilder(PackageElement pkg) {
        return PackageSummaryBuilder.getInstance(context, pkg,
            writerFactory.getPackageSummaryWriter(pkg));
    }

    /**
     * Return the builder that builds the module summary.
     *
     * @param mdle the module being documented.
     * @return the builder that builds the module summary.
     */
    public AbstractBuilder getModuleSummaryBuilder(ModuleElement mdle) {
        return ModuleSummaryBuilder.getInstance(context, mdle,
            writerFactory.getModuleSummaryWriter(mdle));
    }

    /**
     * Return the builder for the class.
     *
     * @param typeElement the class being documented.
     * @param classTree the class tree.
     * @return the writer for the class.  Return null if this
     * writer is not supported by the doclet.
     */
    public AbstractBuilder getClassBuilder(TypeElement typeElement, ClassTree classTree) {
        return ClassBuilder.getInstance(context, typeElement,
            writerFactory.getClassWriter(typeElement, classTree));
    }

    /**
     * Return an instance of the method builder for the given class.
     *
     * @param classWriter the writer for the enclosing class
     * @return an instance of the method builder for the given class.
     */
    public AbstractMemberBuilder getMethodBuilder(ClassWriter classWriter) {
        return MethodBuilder.getInstance(context, classWriter.getTypeElement(),
            writerFactory.getMethodWriter(classWriter));
    }

    /**
     * Return an instance of the annotation type member builder for the given
     * class.
     *
     * @param classWriter the writer for the enclosing annotation type
     * @return an instance of the annotation type member builder for the given
     *         annotation type.
     */
    public AbstractMemberBuilder getAnnotationTypeOptionalMemberBuilder(
            ClassWriter classWriter) {
        return AnnotationTypeOptionalMemberBuilder.getInstance(context,
            classWriter.getTypeElement(),
            writerFactory.getAnnotationTypeOptionalMemberWriter(classWriter));
    }

    /**
     * Return an instance of the annotation type member builder for the given
     * class.
     *
     * @param classWriter the writer for the enclosing annotation type
     * @return an instance of the annotation type member builder for the given
     *         annotation type.
     */
    public AbstractMemberBuilder getAnnotationTypeRequiredMemberBuilder(
            ClassWriter classWriter) {
        return AnnotationTypeRequiredMemberBuilder.getInstance(context,
            classWriter.getTypeElement(),
            writerFactory.getAnnotationTypeRequiredMemberWriter(classWriter));
    }

    /**
     * Return an instance of the enum constants builder for the given class.
     *
     * @param classWriter the writer for the enclosing class
     * @return an instance of the enum constants builder for the given class.
     */
    public AbstractMemberBuilder getEnumConstantsBuilder(ClassWriter classWriter) {
        return EnumConstantBuilder.getInstance(context, classWriter.getTypeElement(),
                writerFactory.getEnumConstantWriter(classWriter));
    }

    /**
     * Return an instance of the field builder for the given class.
     *
     * @param classWriter the writer for the enclosing class
     * @return an instance of the field builder for the given class.
     */
    public AbstractMemberBuilder getFieldBuilder(ClassWriter classWriter) {
        return FieldBuilder.getInstance(context, classWriter.getTypeElement(),
            writerFactory.getFieldWriter(classWriter));
    }

    /**
     * Return an instance of the property builder for the given class.
     *
     * @param classWriter the writer for the enclosing class
     * @return an instance of the field builder for the given class.
     */
    public AbstractMemberBuilder getPropertyBuilder(ClassWriter classWriter) {
        final PropertyWriter propertyWriter =
                writerFactory.getPropertyWriter(classWriter);
        return PropertyBuilder.getInstance(context,
                                           classWriter.getTypeElement(),
                                           propertyWriter);
    }

    /**
     * Return an instance of the constructor builder for the given class.
     *
     * @param classWriter the writer for the enclosing class
     * @return an instance of the constructor builder for the given class.
     */
    public AbstractMemberBuilder getConstructorBuilder(ClassWriter classWriter) {
        return ConstructorBuilder.getInstance(context, classWriter.getTypeElement(),
            writerFactory.getConstructorWriter(classWriter));
    }

    /**
     * Return an instance of the member summary builder for the given class.
     *
     * @param classWriter the writer for the enclosing class
     * @return an instance of the member summary builder for the given class.
     */
    public MemberSummaryBuilder getMemberSummaryBuilder(ClassWriter classWriter) {
        return MemberSummaryBuilder.getInstance(classWriter, context);
    }

    /**
     * Return the builder that builds the serialized form.
     *
     * @return the builder that builds the serialized form.
     */
    public AbstractBuilder getSerializedFormBuilder() {
        return SerializedFormBuilder.getInstance(context);
    }
}
