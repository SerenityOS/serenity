/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6332964
 * @summary Verify getParameterAnnotations doesn't throw spurious errors
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.util.Arrays;

public class TestParameterAnnotations {
    class Inner {
        public Inner(@Marker Object o) {}
    }

    static class StaticNested {
        public StaticNested(@Marker Object o) {}
    }

    static int visitCtorParameterAnnotations(Class clazz) {
        int errors = 0;
        for(Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            try {
                System.out.printf("%nNormal:  %s%nGeneric: %s%n",
                                  ctor.toString(),
                                  ctor.toGenericString());
                Annotation[][] annotationArray = ctor.getParameterAnnotations();
                System.out.println("\tParameter Annotations: " +
                                   Arrays.deepToString(annotationArray));
            } catch (AnnotationFormatError afe) {
                System.err.println("\tWhoops, got an AnnotationFormatError on " +
                                   ctor.toGenericString());
                errors++;
            }
        }
        return errors;
    }

    public static void main(String... argv) {
        int errors = 0;
        class LocalClass {
            LocalClass(@Marker int i){}
        }

        Object anonymous = new Object() {public String toString(){return "Anonymous";}};

        errors +=
            visitCtorParameterAnnotations(Inner.class);
        errors +=
            visitCtorParameterAnnotations(StaticNested.class);
        errors +=
            visitCtorParameterAnnotations(CustomColors.class);
        errors +=
            visitCtorParameterAnnotations(TestParameterAnnotations.class);
        errors +=
            visitCtorParameterAnnotations(LocalClass.class);
        errors +=
            visitCtorParameterAnnotations(anonymous.getClass());

        if (errors > 0)
            throw new RuntimeException(errors +
                                       " failures calling Constructor.getParameterAnnotations");
    }
}

enum CustomColors {
    FUCHSIA(5),
    MULBERRY(6.0d);
    CustomColors(@Marker int arg) {}
    CustomColors(double arg) {}
}

@Retention(RUNTIME)
    @interface Marker {}
