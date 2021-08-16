/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8209055
 * @summary Verify that speculative symbols are not unnecessarily retained in
 *          the DeferredCompletionFailureHandler
 * @library /tools/javac/lib /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code:+open
 *          jdk.compiler/com.sun.tools.javac.main
 * @run main SymbolsDontCumulate
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.DeferredCompletionFailureHandler;

import toolbox.ToolBox;

public class SymbolsDontCumulate {
    ToolBox tb = new ToolBox();

    void testSymbolsCumulate() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> errors = new ArrayList<>();

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task = (com.sun.source.util.JavacTask)
                    compiler.getTask(null,
                                     fm,
                                     d -> errors.add(d.getCode()),
                                     Arrays.asList("-proc:none"),
                                     Arrays.asList("java.lang.Object"),
                                     null);
            assertNotNull(task.getElements().getTypeElement("java.lang.Object"));
            assertNull(task.getElements().getTypeElement("undef.Undef"));
            assertNotNull(task.getElements().getTypeElement("java.lang.String"));
            assertNull(task.getElements().getTypeElement("undef2.Undef2"));
            DeferredCompletionFailureHandler h = DeferredCompletionFailureHandler.instance(((JavacTaskImpl) task).getContext());
            Field class2Flip = h.userCodeHandler.getClass().getDeclaredField("class2Flip");
            class2Flip.setAccessible(true);
            int size = ((Map<?,?>) class2Flip.get(h.userCodeHandler)).size();
            assertEquals(0, size);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Unexpected value, expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Unexpected value, object: " + obj);
        }
    }

    private static void assertNull(Object obj) {
        if (obj != null) {
            throw new AssertionError("Unexpected value, object: " + obj);
        }
    }

    public static void main(String... args) throws Exception {
        SymbolsDontCumulate t = new SymbolsDontCumulate();
        t.testSymbolsCumulate();
    }

}
