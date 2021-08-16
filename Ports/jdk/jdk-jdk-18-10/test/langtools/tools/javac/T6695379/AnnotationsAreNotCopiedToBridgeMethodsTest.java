/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6695379
 * @summary Copy method annotations and parameter annotations to synthetic
 * bridge methods
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main AnnotationsAreNotCopiedToBridgeMethodsTest
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.io.BufferedInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.util.Assert;

public class AnnotationsAreNotCopiedToBridgeMethodsTest {

    public static void main(String[] args) throws Exception {
        new AnnotationsAreNotCopiedToBridgeMethodsTest().run();
    }

    void run() throws Exception {
        checkClassFile(Paths.get(System.getProperty("test.classes"),
                this.getClass().getSimpleName() + "$CovariantReturnType.class"));
        checkClassFile(Paths.get(System.getProperty("test.classes"),
                this.getClass().getSimpleName() +
                "$CovariantReturnType$VisibilityChange.class"));
    }

    void checkClassFile(final Path cfilePath) throws Exception {
        ClassFile classFile = ClassFile.read(
                new BufferedInputStream(Files.newInputStream(cfilePath)));
        for (Method method : classFile.methods) {
            if (method.access_flags.is(AccessFlags.ACC_BRIDGE)) {
                checkForAttr(method.attributes,
                        "Annotations hasn't been copied to bridge method",
                        Attribute.RuntimeVisibleAnnotations,
                        Attribute.RuntimeVisibleParameterAnnotations);
            }
        }
    }

    void checkForAttr(Attributes attrs, String errorMsg, String... attrNames) {
        for (String attrName : attrNames) {
            Assert.checkNonNull(attrs.get(attrName), errorMsg);
        }
    }

    @Target(value = {ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface ParamAnnotation {}

    @Target(value = {ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MethodAnnotation {}

    abstract class T<A,B> {
        B m(A a){return null;}
    }

    class CovariantReturnType extends T<Integer, Integer> {
        @MethodAnnotation
        Integer m(@ParamAnnotation Integer i) {
            return i;
        }

        public class VisibilityChange extends CovariantReturnType {}

    }

}
