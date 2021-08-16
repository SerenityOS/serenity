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
 * @bug 8011383
 * @summary Symbol.getModifiers omits ACC_ABSTRACT from interface with default methods
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.lang.model.element.*;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Assert;

public class DefaultMethodFlags {

    public static void main(String[] args) throws IOException {
        new DefaultMethodFlags().run(args);
    }

    void run(String[] args) throws IOException {
        checkDefaultMethodFlags();
    }

    void checkDefaultMethodFlags() throws IOException {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> fos =
                    fm.getJavaFileObjectsFromFiles(
                    Arrays.asList(new File(
                    System.getProperty("test.src"),
                    this.getClass().getSimpleName() + ".java")));
            JavacTask task = (JavacTask) c.getTask(null, fm, null, null, null, fos);

            task.addTaskListener(new TaskListener() {

                @Override
                public void started(TaskEvent e) {}

                @Override
                public void finished(TaskEvent e) {
                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        TypeElement te = e.getTypeElement();
                        if (te.getSimpleName().toString().equals("I")) {
                            checkDefaultInterface(te);
                        }
                    }
                }
            });

            task.analyze();
        }
    }

    void checkDefaultInterface(TypeElement te) {
        System.err.println("Checking " + te.getSimpleName());
        Assert.check(te.getModifiers().contains(Modifier.ABSTRACT));
        for (Element e : te.getEnclosedElements()) {
            if (e.getSimpleName().toString().matches("(\\w)_(default|static|abstract)")) {
                boolean abstractExpected = false;
                String methodKind = e.getSimpleName().toString().substring(2);
                switch (methodKind) {
                    case "default":
                    case "static":
                        break;
                    case "abstract":
                        abstractExpected = true;
                        break;
                    default:
                        Assert.error("Cannot get here!" + methodKind);
                }
                Assert.check(e.getModifiers().contains(Modifier.ABSTRACT) == abstractExpected);
            }
        }
    }
}

interface I {
    default void m_default() { }
    static void m_static() { }
    void m_abstract();
}
