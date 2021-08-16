/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.classfile;

/**
 * Visitor with empty method bodies, can be extended and used in conjunction
 * with the DescendingVisitor class, e.g. By courtesy of David Spencer.
 *
 * @see DescendingVisitor
 */
public class EmptyVisitor implements Visitor
{
    protected EmptyVisitor()
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitAnnotation(final Annotations obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitParameterAnnotation(final ParameterAnnotations obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitAnnotationEntry(final AnnotationEntry obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitAnnotationDefault(final AnnotationDefault obj)
    {
    }

    @Override
    public void visitCode(final Code obj)
    {
    }

    @Override
    public void visitCodeException(final CodeException obj)
    {
    }

    @Override
    public void visitConstantClass(final ConstantClass obj)
    {
    }

    @Override
    public void visitConstantDouble(final ConstantDouble obj)
    {
    }

    @Override
    public void visitConstantFieldref(final ConstantFieldref obj)
    {
    }

    @Override
    public void visitConstantFloat(final ConstantFloat obj)
    {
    }

    @Override
    public void visitConstantInteger(final ConstantInteger obj)
    {
    }

    @Override
    public void visitConstantInterfaceMethodref(final ConstantInterfaceMethodref obj)
    {
    }

    @Override
    public void visitConstantInvokeDynamic(final ConstantInvokeDynamic obj)
    {
    }

    @Override
    public void visitConstantLong(final ConstantLong obj)
    {
    }

    @Override
    public void visitConstantMethodref(final ConstantMethodref obj)
    {
    }

    @Override
    public void visitConstantNameAndType(final ConstantNameAndType obj)
    {
    }

    @Override
    public void visitConstantPool(final ConstantPool obj)
    {
    }

    @Override
    public void visitConstantString(final ConstantString obj)
    {
    }

    @Override
    public void visitConstantUtf8(final ConstantUtf8 obj)
    {
    }

    @Override
    public void visitConstantValue(final ConstantValue obj)
    {
    }

    @Override
    public void visitDeprecated(final Deprecated obj)
    {
    }

    @Override
    public void visitExceptionTable(final ExceptionTable obj)
    {
    }

    @Override
    public void visitField(final Field obj)
    {
    }

    @Override
    public void visitInnerClass(final InnerClass obj)
    {
    }

    @Override
    public void visitInnerClasses(final InnerClasses obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitBootstrapMethods(final BootstrapMethods obj)
    {
    }

    @Override
    public void visitJavaClass(final JavaClass obj)
    {
    }

    @Override
    public void visitLineNumber(final LineNumber obj)
    {
    }

    @Override
    public void visitLineNumberTable(final LineNumberTable obj)
    {
    }

    @Override
    public void visitLocalVariable(final LocalVariable obj)
    {
    }

    @Override
    public void visitLocalVariableTable(final LocalVariableTable obj)
    {
    }

    @Override
    public void visitMethod(final Method obj)
    {
    }

    @Override
    public void visitSignature(final Signature obj)
    {
    }

    @Override
    public void visitSourceFile(final SourceFile obj)
    {
    }

    @Override
    public void visitSynthetic(final Synthetic obj)
    {
    }

    @Override
    public void visitUnknown(final Unknown obj)
    {
    }

    @Override
    public void visitStackMap(final StackMap obj)
    {
    }

    @Override
    public void visitStackMapEntry(final StackMapEntry obj)
    {
    }

    /**
     * @since 6.0
    @Override
    public void visitStackMapTable(StackMapTable obj)
    {
    }
     */

    /**
     * @since 6.0
    @Override
    public void visitStackMapTableEntry(StackMapTableEntry obj)
    {
    }
     */

    /**
     * @since 6.0
     */
    @Override
    public void visitEnclosingMethod(final EnclosingMethod obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitLocalVariableTypeTable(final LocalVariableTypeTable obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitMethodParameters(final MethodParameters obj)
    {
    }

    /**
     * @since 6.4.0
     */
    @Override
    public void visitMethodParameter(final MethodParameter obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitConstantMethodType(final ConstantMethodType obj)
    {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitConstantMethodHandle(final ConstantMethodHandle constantMethodHandle) {
    }

    /**
     * @since 6.0
     */
    @Override
    public void visitParameterAnnotationEntry(final ParameterAnnotationEntry parameterAnnotationEntry) {
    }

    /**
     * @since 6.1
     */
    @Override
    public void visitConstantPackage(final ConstantPackage constantPackage) {
    }

    /**
     * @since 6.1
     */
    @Override
    public void visitConstantModule(final ConstantModule constantModule) {
    }

    /**
     * @since 6.3
     */
    @Override
    public void visitConstantDynamic(final ConstantDynamic obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModule(final Module obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModuleRequires(final ModuleRequires obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModuleExports(final ModuleExports obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModuleOpens(final ModuleOpens obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModuleProvides(final ModuleProvides obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModulePackages(final ModulePackages obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitModuleMainClass(final ModuleMainClass obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitNestHost(final NestHost obj) {
    }

    /** @since 6.4.0 */
    @Override
    public void visitNestMembers(final NestMembers obj) {
    }
}
