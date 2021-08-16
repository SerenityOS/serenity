/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146726
 * @summary Test that warnings about repeated supported options and annotation types output as expected.
 * @compile TestRepeatedSupportedItems.java
 * @compile/ref=au_8.out       -XDrawDiagnostics -processor TestRepeatedSupportedItems -proc:only  -source 8 -Xlint:-options TestRepeatedSupportedItems.java
 * @compile/ref=au_current.out -XDrawDiagnostics -processor TestRepeatedSupportedItems -proc:only            -Xlint:-options TestRepeatedSupportedItems.java
 */

import java.lang.annotation.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A warning should be issued by the logic in
 * javax.annotation.processing.AbstractProcessor for the repeated
 * information.  The "Foo" option warnings occur regardless of source
 * level. The number of times the Baz annotation type is repeated
 * depends on whether or not the source level supports modules.
 */
@SupportedAnnotationTypes({"foo/Baz", "foo/Baz", "bar/Baz", "Baz", "Baz"})
@SupportedOptions({"Foo", "Foo"})
@Baz
public class TestRepeatedSupportedItems extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment) {
        return true;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Baz {
}
