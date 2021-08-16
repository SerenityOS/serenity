/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8040822
 * @summary Check that all TaskEvents are balanced.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import javax.tools.*;

import com.sun.source.util.*;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.CompileStates.CompileState;

public class EventsBalancedTest {
    JavacTool tool = (JavacTool) ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null);

    public static void main(String... args) throws IOException {
        EventsBalancedTest t = new EventsBalancedTest();
        try {
            t.test();
        } finally {
            t.fm.close();
        }
    }

    void test() throws IOException {
        TestSource a = new TestSource("B", "class B extends A { }");
        TestSource b = new TestSource("A", "abstract class A { }");

        test(null, Arrays.asList(a, b));
        test(null, Arrays.asList(b, a));

        for (CompileState stop : CompileState.values()) {
            test(Arrays.asList("--should-stop=ifNoError=" + stop,
                               "--should-stop=ifError=" + stop),
                 Arrays.asList(a, b));
            test(Arrays.asList("--should-stop=ifNoError=" + stop,
                               "--should-stop=ifError=" + stop),
                 Arrays.asList(b, a));
        }
    }

    void test(List<String> options, List<JavaFileObject> files) throws IOException {
        System.err.println("testing: " + options + ", " + files);
        TestListener listener = new TestListener();
        JavacTask task = tool.getTask(null, fm, null, options, null, files);

        task.setTaskListener(listener);

        task.call();

        for (Entry<Kind, Integer> e : listener.kind2Count.entrySet()) {
            if (e.getValue() != null && e.getValue() != 0) {
                throw new IllegalStateException("Not balanced event: " + e.getKey());
            }
        }
    }

    static class TestListener implements TaskListener {
        final Map<Kind, Integer> kind2Count = new HashMap<>();

        int get(Kind k) {
            Integer count = kind2Count.get(k);

            if (count == null)
                kind2Count.put(k, count = 0);

            return count;
        }

        @Override
        public void started(TaskEvent e) {
            kind2Count.put(e.getKind(), get(e.getKind()) + 1);
        }

        @Override
        public void finished(TaskEvent e) {
            int count = get(e.getKind());

            if (count <= 0)
                throw new IllegalStateException("count<=0 for: " + e.getKind());

            kind2Count.put(e.getKind(), count - 1);
        }

    }
    static class TestSource extends SimpleJavaFileObject {
        final String content;
        public TestSource(String fileName, String content) {
            super(URI.create("myfo:/" + fileName + ".java"), JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

}
