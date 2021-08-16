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
 * @bug 7071377
 * @summary verify if erroneous class names are rejected
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.main
 * @build TestClassNames JavacTestingAbstractProcessor CompileFail
 * @run main CompileFail ERROR  -processor TestClassNames TestClassNames.x.y
 * @run main CompileFail ERROR  -processor TestClassNames x.y.TestClassNames
 * @run main CompileFail ERROR  -processor NoClass NoClass.x.y
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;

/**
 * No-op processor; should not be run.
 */
public class TestClassNames extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment) {
        return true;
    }
}
