/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import com.sun.tools.classfile.*;

/**
 * Trivial {@code Attribute.Visitor} implementation, to make it easy to
 * write visitors for specific attributes.
 */
class AttributeVisitor<R, P> implements Attribute.Visitor<R, P> {
    public R visitBootstrapMethods(BootstrapMethods_attribute attr, P p) { return null; }
    public R visitDefault(DefaultAttribute attr, P p) { return null; }
    public R visitAnnotationDefault(AnnotationDefault_attribute attr, P p) { return null; }
    public R visitCharacterRangeTable(CharacterRangeTable_attribute attr, P p) { return null; }
    public R visitCode(Code_attribute attr, P p) { return null; }
    public R visitCompilationID(CompilationID_attribute attr, P p) { return null; }
    public R visitConstantValue(ConstantValue_attribute attr, P p) { return null; }
    public R visitDeprecated(Deprecated_attribute attr, P p) { return null; }
    public R visitEnclosingMethod(EnclosingMethod_attribute attr, P p) { return null; }
    public R visitExceptions(Exceptions_attribute attr, P p) { return null; }
    public R visitInnerClasses(InnerClasses_attribute attr, P p) { return null; }
    public R visitLineNumberTable(LineNumberTable_attribute attr, P p) { return null; }
    public R visitLocalVariableTable(LocalVariableTable_attribute attr, P p) { return null; }
    public R visitLocalVariableTypeTable(LocalVariableTypeTable_attribute attr, P p) { return null; }
    public R visitNestHost(NestHost_attribute attr, P p) { return null; }
    public R visitMethodParameters(MethodParameters_attribute attr, P p) { return null; }
    public R visitModule(Module_attribute attr, P p) { return null; }
    public R visitModuleHashes(ModuleHashes_attribute attr, P p) { return null; }
    public R visitModuleMainClass(ModuleMainClass_attribute attr, P p) { return null; }
    public R visitModulePackages(ModulePackages_attribute attr, P p) { return null; }
    public R visitModuleResolution(ModuleResolution_attribute attr, P p) { return null; }
    public R visitModuleTarget(ModuleTarget_attribute attr, P p) { return null; }
    public R visitNestMembers(NestMembers_attribute attr, P p) { return null; }
    public R visitRuntimeVisibleAnnotations(RuntimeVisibleAnnotations_attribute attr, P p) { return null; }
    public R visitRuntimeInvisibleAnnotations(RuntimeInvisibleAnnotations_attribute attr, P p) { return null; }
    public R visitRuntimeVisibleParameterAnnotations(RuntimeVisibleParameterAnnotations_attribute attr, P p) { return null; }
    public R visitRuntimeInvisibleParameterAnnotations(RuntimeInvisibleParameterAnnotations_attribute attr, P p) { return null; }
    public R visitRuntimeVisibleTypeAnnotations(RuntimeVisibleTypeAnnotations_attribute attr, P p) { return null; }
    public R visitRuntimeInvisibleTypeAnnotations(RuntimeInvisibleTypeAnnotations_attribute attr, P p) { return null; }
    public R visitSignature(Signature_attribute attr, P p) { return null; }
    public R visitSourceDebugExtension(SourceDebugExtension_attribute attr, P p) { return null; }
    public R visitSourceFile(SourceFile_attribute attr, P p) { return null; }
    public R visitSourceID(SourceID_attribute attr, P p) { return null; }
    public R visitStackMap(StackMap_attribute attr, P p) { return null; }
    public R visitStackMapTable(StackMapTable_attribute attr, P p) { return null; }
    public R visitSynthetic(Synthetic_attribute attr, P p) { return null; }
    public R visitPermittedSubclasses(PermittedSubclasses_attribute attr, P p) { return null; }
    public R visitRecord(Record_attribute attr, P p) { return null; }
}
