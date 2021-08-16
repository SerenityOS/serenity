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
 * @bug 8009367
 * @summary Test that the correct kind of names (binary) are used when comparing
 *          Class and Symbol for repeatable Classes.
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @build   JavacTestingAbstractProcessor TestQualifiedNameUsed p.Q p.QQ p.R p.RR
 * @run compile -XDaccessInternalAPI -processor TestQualifiedNameUsed -proc:only TestQualifiedNameUsed.java
 */

import java.lang.annotation.Repeatable;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.lang.model.util.ElementFilter.*;

import com.sun.tools.javac.util.Assert;

public class TestQualifiedNameUsed extends JavacTestingAbstractProcessor {

    @Q
    @p.Q
    @p.R.Q
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            boolean hasRun = false;
            for (Element element : roundEnv.getRootElements()) {
                for (ExecutableElement e : methodsIn(element.getEnclosedElements())) {
                    if (e.getSimpleName().contentEquals("value"))
                        continue; // don't want to look Q.value() in this file

                    hasRun = true;
                    Q[] qs = e.getAnnotationsByType(Q.class);
                    Assert.check(qs.length == 1);
                    Assert.check(qs[0] instanceof Q);

                    p.Q[] ps = e.getAnnotationsByType(p.Q.class);
                    Assert.check(ps.length == 1);
                    Assert.check(ps[0] instanceof p.Q);

                    p.R.Q[] rs = e.getAnnotationsByType(p.R.Q.class);
                    Assert.check(rs.length == 1);
                    Assert.check(rs[0] instanceof p.R.Q);
                }
            }
            if (!hasRun) throw new RuntimeException("No methods!");
        }
        return true;
    }
}

@Repeatable(QQ.class)
@interface Q {}

@interface QQ {
    Q[] value();
}
