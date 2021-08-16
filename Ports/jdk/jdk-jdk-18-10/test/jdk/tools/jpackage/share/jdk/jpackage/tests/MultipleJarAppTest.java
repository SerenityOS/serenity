/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.tests;

import java.nio.file.Path;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.JPackageCommand;

/*
 * @test
 * @summary jpackage application packed in multiple jars
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile MultipleJarAppTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.MultipleJarAppTest
 */

public final class MultipleJarAppTest {

    @Test
    @Parameter("B")
    @Parameter("C")
    public void test(String mainClass) {
        JPackageCommand cmd = JPackageCommand.helloAppImage("a.jar:A");
        HelloApp.createBundle(JavaAppDesc.parse("b.jar:B"), cmd.inputDir());
        HelloApp.createBundle(JavaAppDesc.parse("c.jar:C"), cmd.inputDir());

        cmd.setArgumentValue("--main-class", mainClass);
        cmd.executeAndAssertHelloAppImageCreated();
    }
}
