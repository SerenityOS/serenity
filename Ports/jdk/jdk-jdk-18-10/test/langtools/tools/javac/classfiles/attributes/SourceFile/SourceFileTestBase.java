/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.SourceFile_attribute;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaFileObject;

import toolbox.ToolBox;

/**
 * Base class for Source file attribute tests. Checks expected file name for specified classes in the SourceFile attribute.
 * To add new tests you should extend the SourceFileTestBase class and invoke {@link #test} for static sources
 * or {@link #compileAndTest} for generated sources. For more information see corresponding methods.
 *
 * @see #test
 * @see #compileAndTest
 */
public class SourceFileTestBase extends TestBase {
    /**
     * Checks expected fileName for the specified class in the SourceFile attribute.
     *
     * @param classToTest class to check its SourceFile attribute
     * @param fileName    expected name of the file from which the test file is compiled.
     */
    protected void test(Class<?> classToTest, String fileName) throws Exception {
        assertAttributePresent(ClassFile.read(getClassFile(classToTest)), fileName);
    }

    /**
     * Checks expected fileName for the specified class in the SourceFile attribute.
     *
     * @param classToTest class name to check its SourceFile attribute
     * @param fileName    expected name of the file from which the test file is compiled.
     */
    protected void test(String classToTest, String fileName) throws Exception {
        assertAttributePresent(ClassFile.read(getClassFile(classToTest + ".class")), fileName);
    }

    /**
     * Checks expected fileName for the specified class in the SourceFile attribute.
     *
     * @param classToTest path of class to check its SourceFile attribute
     * @param fileName    expected name of the file from which the test file is compiled.
     */
    protected void test(Path classToTest, String fileName) throws Exception {
        assertAttributePresent(ClassFile.read(classToTest), fileName);
    }

    /**
     * Compiles sourceCode and for each specified class name checks the SourceFile attribute.
     * The file name is extracted from source code.
     *
     * @param sourceCode    source code to compile
     * @param classesToTest class names to check their SourceFile attribute.
     */
    protected void compileAndTest(String sourceCode, String... classesToTest) throws Exception {

        Map<String, ? extends JavaFileObject> classes = compile(sourceCode).getClasses();
        String fileName = ToolBox.getJavaFileNameFromSource(sourceCode);
        for (String className : classesToTest) {
            assertAttributePresent(ClassFile.read(classes.get(className).openInputStream()), fileName);
        }
    }

    private void assertAttributePresent(ClassFile classFile, String fileName) throws Exception {

        //We need to count attributes with the same names because there is no appropriate API in the ClassFile.

        List<SourceFile_attribute> sourceFileAttributes = new ArrayList<>();
        for (Attribute a : classFile.attributes.attrs) {
            if (Attribute.SourceFile.equals(a.getName(classFile.constant_pool))) {
                sourceFileAttributes.add((SourceFile_attribute) a);
            }
        }

        assertEquals(sourceFileAttributes.size(), 1, "Should be the only SourceFile attribute");

        SourceFile_attribute attribute = sourceFileAttributes.get(0);

        assertEquals(classFile.constant_pool.getUTF8Info(attribute.attribute_name_index).value,
                Attribute.SourceFile, "Incorrect attribute name");
        assertEquals(classFile.constant_pool.getUTF8Info(attribute.sourcefile_index).value, fileName,
                "Incorrect source file name");
        assertEquals(attribute.attribute_length, 2, "Incorrect attribute length");
    }
}
