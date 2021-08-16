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

package jdk.javadoc.internal.doclets.toolkit;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;

/**
 * The interface for a factory creates writers.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public interface WriterFactory {

    /**
     * Return the writer for the constant summary.
     *
     * @return the writer for the constant summary.  Return null if this
     * writer is not supported by the doclet.
     */
    ConstantsSummaryWriter getConstantsSummaryWriter();

    /**
     * Return the writer for the package summary.
     *
     * @param packageElement the package being documented
     * @return the writer for the package summary.  Return null if this
     * writer is not supported by the doclet.
     */
    PackageSummaryWriter getPackageSummaryWriter(PackageElement packageElement);

    /**
     * Return the writer for the module summary.
     *
     * @param mdle the module being documented
     * @return the writer for the module summary.  Return null if this
     * writer is not supported by the doclet.
     */
    ModuleSummaryWriter getModuleSummaryWriter(ModuleElement mdle);

    /**
     * Returns the writer for a given type element,
     * or null if this writer is not supported by the doclet.
     *
     * @param typeElement the class being documented
     * @param classTree   the class tree
     * @return the writer
     */
    ClassWriter getClassWriter(TypeElement typeElement, ClassTree classTree);

    /**
     * Return the method writer for a given type element,
     * or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the class being documented
     * @return the method writer
     */
    MethodWriter getMethodWriter(ClassWriter classWriter);

    /**
     * Return the annotation type optional member writer for a given annotation
     * type, or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the annotation type being documented
     * @return the member writer
     */
    AnnotationTypeOptionalMemberWriter getAnnotationTypeOptionalMemberWriter(
            ClassWriter classWriter);

    /**
     * Return the annotation type required member writer for a given annotation
     * type, or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the annotation type being documented
     * @return the member writer
     */
    AnnotationTypeRequiredMemberWriter getAnnotationTypeRequiredMemberWriter(
            ClassWriter classWriter);

    /**
     * Return the enum constant writer for a given type element,
     * or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the type element being documented
     * @return the enum constant writer
     */
    EnumConstantWriter getEnumConstantWriter(ClassWriter classWriter);

    /**
     * Return the field writer for a given type element,
     * or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the class being documented
     * @return the field writer for the given class.  Return null if this
     * writer is not supported by the doclet.
     */
    FieldWriter getFieldWriter(ClassWriter classWriter);

    /**
     * Return the property writer for a given class,
     * or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the type element being documented
     * @return the property writer
     */
    PropertyWriter getPropertyWriter(ClassWriter classWriter);

    /**
     * Return the constructor writer for a given type element,
     * or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the type element being documented
     * @return the constructor writer
     */
    ConstructorWriter getConstructorWriter(ClassWriter classWriter);

    /**
     * Return the specified member summary writer for a given type element,
     * or null if this writer is not supported by the doclet.
     *
     * @param classWriter the writer for the class being documented
     * @param memberType  the {@link VisibleMemberTable} member type indicating
     *                    the type of member summary that should be returned
     * @return the summary writer
     *
     * @see VisibleMemberTable
     */
    MemberSummaryWriter getMemberSummaryWriter(ClassWriter classWriter,
                                               VisibleMemberTable.Kind memberType);

    /**
     * Return the writer for the serialized form.
     *
     * @return the writer for the serialized form
     */
    SerializedFormWriter getSerializedFormWriter();

    /**
     * Return the handler for doc files.
     *
     * @return the handler for the doc files
     */
    DocFilesHandler getDocFilesHandler(Element pkg);
}
