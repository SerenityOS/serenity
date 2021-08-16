/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Test for field annotations.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/FieldAnnotationsApp.java test-classes/MyAnnotation.java
 * @run driver FieldAnnotationsTest
 */

import jdk.test.lib.process.OutputAnalyzer;

// This is a test for the handling of multi-dimensional Arrays in MetaspaceClosure.
//
// We choose FieldAnnotations because they happen to be implemented as a multi-dimension
// Array (Annotations::_fields_annotations, which is of type Array<Array<unsigned char>*>*,
// and is handled by the template class PointerArrayRef<T> in metaspaceClosure.hpp).
//
// Specifically, we are testing the following C code, where _fields_annotations is non-NULL:
//
// void Annotations::metaspace_pointers_do(MetaspaceClosure* it) {
//   ...
//   it->push(&_fields_annotations);
//
// which will be matched with the function
//
// template <typename T> void MetaspaceClosure::push(Array<T*>** mpp, Writability w = _default)
//
public class FieldAnnotationsTest {
    public static void main(String[] args) throws Exception {
        String[] ARCHIVE_CLASSES = {"FieldAnnotationsApp", "MyAnnotation"};
        String appJar = JarBuilder.build("FieldAnnotationsTest", ARCHIVE_CLASSES);

        OutputAnalyzer dumpOutput = TestCommon.dump(
                appJar, ARCHIVE_CLASSES);
        TestCommon.checkDump(dumpOutput);

        OutputAnalyzer execOutput = TestCommon.exec(appJar, "FieldAnnotationsApp");
        TestCommon.checkExec(execOutput, "Field annotations are OK.");
    }
}
