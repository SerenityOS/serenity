/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     6341534
 * @summary PackageElement.getEnclosedElements results in NullPointerException from parse(JavaCompiler.java:429)
 * @author  Steve Sides
 * @author  Peter von der Ahe
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile T6341534.java
 * @compile -proc:only -processor T6341534 dir/package-info.java
 * @compile -processor T6341534 dir/package-info.java
 */

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import java.util.*;
import java.util.Set;
import static javax.tools.Diagnostic.Kind.*;

public class T6341534 extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv)  {
        messager.printMessage(NOTE,
                              String.valueOf(eltUtils.getPackageElement("no.such.package")));
        PackageElement dir = eltUtils.getPackageElement("dir");
        messager.printMessage(NOTE, dir.getQualifiedName().toString());
        for (Element e : dir.getEnclosedElements())
            messager.printMessage(NOTE, e.toString());
        return true;
    }
}
