/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Regression test for:
 * Javadoc does not process base class. If user specifies few classes on the
 * command line and few packages, with a situation where one of the specified
 * classes(on the command line) extends a class from one of the packages, then
 * due to some anomaly in ordering in which all the class and package objects
 * get constructed, few classes were getting marked as "not included", even
 * thought they were included in this run and hence documentation for those
 * packages was wrong. The test case for which javadoc was failing is given
 * in bug# 4197513.
 *
 * @bug 4197513
 * @summary Javadoc does not process base class.
 * @build BaseClass.java
 */

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import jdk.javadoc.doclet.*;

public class BaseClass implements Doclet {

    public boolean run(DocletEnvironment root) {
        Elements elementUtils = root.getElementUtils();
        TypeElement klass = elementUtils.getTypeElement("baz.Foo");
        if (!root.isIncluded(klass)) {
            throw new AssertionError("Base class is not included: baz.Foo");
        }

        for (TypeElement te : ElementFilter.typesIn(root.getSpecifiedElements())) {
            if (te.getKind() == ElementKind.CLASS &&
                    te.getSimpleName().contentEquals("Bar")) {
                klass = te;
            }
        }
        if (klass == null) {
            throw new AssertionError("class Bar not found");
        }
        List<? extends Element> members = klass.getEnclosedElements();


        boolean foundPublic = false;
        boolean foundProtected = false;

        boolean foundPackagePrivate = false;
        boolean foundPrivate = false;

        List<Element> included = members.stream()
                .filter(cls -> root.isIncluded(cls))
                .collect(Collectors.toList());

        for (Element e : included) {
            System.out.println("element: " + e);
            if (e.getSimpleName().toString().equals("aPublicMethod")) {
                foundPublic = true;
            }
            if (e.getSimpleName().toString().equals("aProtectedMethod")) {
                foundProtected = true;
            }
            if (e.getSimpleName().toString().equals("aPackagePrivateMethod")) {
                foundPackagePrivate = true;
            }
            if (e.getSimpleName().toString().equals("aPackagePrivateMethod")) {
                foundPrivate = true;
            }
        }
        if (!foundPublic || !foundProtected) {
            throw new AssertionError("selected methods not found");
        }

        if (foundPrivate || foundPackagePrivate) {
             throw new AssertionError("unselected methods found");
        }

        return true;
    }

    public Set<Doclet.Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    public void init(Locale locale, Reporter reporter) {
        return;
    }

    @Override
    public String getName() {
        return "BaseClass";
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
