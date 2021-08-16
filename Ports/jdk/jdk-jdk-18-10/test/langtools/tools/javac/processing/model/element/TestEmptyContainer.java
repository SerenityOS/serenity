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
 * @bug 8026857
 * @summary Test that an empty container does not stop us from looking at
 *          supertypes for inherited repeated annotations.
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @build   JavacTestingAbstractProcessor TestEmptyContainer
 * @compile -XDaccessInternalAPI -processor TestEmptyContainer -proc:only TestEmptyContainer.java
 */

import com.sun.tools.javac.util.Assert;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import static javax.lang.model.util.ElementFilter.*;

@TestEmptyContainer.Foo(1)
public class TestEmptyContainer extends JavacTestingAbstractProcessor {

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            boolean hasRun = false;
            for (Element element : roundEnv.getRootElements())
                for (TypeElement te : typesIn(element.getEnclosedElements()))
                    if (te.getQualifiedName().contentEquals("TestEmptyContainer.T2")) {
                        hasRun = true;
                        Foo[] foos = te.getAnnotationsByType(Foo.class);
                        System.out.println("  " + te);
                        System.out.println("  " + Arrays.asList(foos));
                        Assert.check(foos.length == 1, "Should find one @Foo");
                        Assert.check(foos[0].value() == 1, "Should find @Foo(1)");
                    }
            if (!hasRun)
                throw new RuntimeException("Annotation processor couldn't find class T2, test broken!");
        }
        return true;
    }

    // This empty container should not stop us from finding @Foo(1) on TestEmptyContainer above
    @TestEmptyContainer.FooContainer({})
    public static class T2 extends TestEmptyContainer {
    }

    @Repeatable(FooContainer.class)
    @Inherited
    public static @interface Foo {
        int value();
    }

    @Inherited
    public static @interface FooContainer {
        Foo[] value();
    }
}
