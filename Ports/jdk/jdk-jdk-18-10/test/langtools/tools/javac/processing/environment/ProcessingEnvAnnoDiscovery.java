/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8038080
 * @summary make sure that all declaration annotations are discovered
 *          by the processing environment
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor ProcessingEnvAnnoDiscovery
 * @compile/process -XDaccessInternalAPI -processor ProcessingEnvAnnoDiscovery ProcessingEnvAnnoDiscovery.java
 */

import java.lang.annotation.*;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;

import com.sun.tools.javac.util.*;

@ProcessingEnvAnnoDiscovery.Anno1
public class ProcessingEnvAnnoDiscovery<@ProcessingEnvAnnoDiscovery.Anno4 T>
        extends JavacTestingAbstractProcessor {
    private int round = 0;

    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment rEnv) {
        if (round++ == 0) {
            System.out.println(annos);
            Assert.check(annos.contains(eltUtils.getTypeElement("java.lang.annotation.Target")));
            Assert.check(annos.contains(eltUtils.getTypeElement("ProcessingEnvAnnoDiscovery.Anno1")));
            Assert.check(annos.contains(eltUtils.getTypeElement("ProcessingEnvAnnoDiscovery.Anno2")));
            Assert.check(annos.contains(eltUtils.getTypeElement("ProcessingEnvAnnoDiscovery.Anno3")));
            Assert.check(annos.contains(eltUtils.getTypeElement("ProcessingEnvAnnoDiscovery.Anno4")));
            Assert.check(annos.contains(eltUtils.getTypeElement("ProcessingEnvAnnoDiscovery.Anno5")));
            Assert.check(annos.size() == 6, "Found extra annotations"); //Anno1-5 + @Target
        }

        return true;
    }

    @Anno2
    public <@Anno5 K> K m(@Anno3 long foo) {
        return null;
    }

    @interface Anno1 {}

    @interface Anno2 {}

    @interface Anno3 {}

    @Target(ElementType.TYPE_PARAMETER)
    @interface Anno4 {}

    @Target(ElementType.TYPE_PARAMETER)
    @interface Anno5 {}

}
