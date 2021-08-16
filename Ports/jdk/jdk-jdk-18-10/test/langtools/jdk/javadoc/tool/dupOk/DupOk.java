/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4673477
 * @summary The first definition found for each class should be documented
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.File;
import java.util.*;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;

public class DupOk implements Doclet
{
    public static void main(String[] args) {
        File srcFile = new File(System.getProperty("test.src", "."));
        String path1 = new File(srcFile, "sp1").getPath();
        String path2 = new File(srcFile, "sp2").getPath();
        String[] aargs = {
            "-docletpath",
            new File(System.getProperty("test.classes", ".")).getPath(),
            "-doclet",
            "DupOk",
            "-sourcepath",
            path1 + System.getProperty("path.separator") + path2,
            "p"
        };
        // run javadoc on package p
        if (jdk.javadoc.internal.tool.Main.execute(aargs) != 0)
            throw new Error();
    }

    public boolean run(DocletEnvironment root) {
        Set<TypeElement> classes = ElementFilter.typesIn(root.getIncludedElements());
        if (classes.size() != 2)
            throw new Error("1 " + Arrays.asList(classes));
        for (TypeElement clazz : classes) {
            if (getFields(clazz).size() != 1)
                throw new Error("2 " + clazz + " " + getFields(clazz));
        }
        return true;
    }

    List<Element> getFields(TypeElement klass) {
        List<Element> out = new ArrayList<>();
        for (Element e : klass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        return;
    }
}
