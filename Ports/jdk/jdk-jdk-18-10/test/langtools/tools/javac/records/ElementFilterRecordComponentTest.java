/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8241312 8246774
 * @summary test for javax.lang.model.util.ElementFilter::recordComponentsIn
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.util.Assert;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;


public class ElementFilterRecordComponentTest {
    public static void main(String... args) throws IOException {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTask t = (JavacTask) c.getTask(null, null, null, null, null,
                List.of(new SimpleJavaFileObject(URI.create("TestClass.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return "record R(int val1, int val2) {}";
            }
        }));
        TypeElement record = (TypeElement) t.analyze().iterator().next();
        Set<RecordComponentElement> recordSet = ElementFilter.recordComponentsIn(new HashSet<>(record.getEnclosedElements()));
        Assert.check(recordSet.size() == 2);
        List<RecordComponentElement> recordList = ElementFilter.recordComponentsIn(record.getEnclosedElements());
        Assert.check(recordList.size() == 2);
        Assert.check(recordSet.containsAll(recordList));
    }
}

