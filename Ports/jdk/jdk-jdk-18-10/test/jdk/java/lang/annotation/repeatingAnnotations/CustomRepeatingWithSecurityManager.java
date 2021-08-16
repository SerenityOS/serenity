/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8073056
 * @summary Repeating annotations throws java.security.AccessControlException with a SecurityManager
 *
 * @library /test/lib
 * @build jdk.test.lib.Asserts
 * @run main CustomRepeatingWithSecurityManager
 * @run main/othervm -Djava.security.manager=allow CustomRepeatingWithSecurityManager "withSM"
 */

import java.lang.annotation.*;
import java.lang.reflect.*;

import jdk.test.lib.Asserts;

public class CustomRepeatingWithSecurityManager {
    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            SecurityManager sm = new SecurityManager();
            System.setSecurityManager(sm);
        }

        Asserts.assertTrue(new CustomAnnotations().getAnnotationsByType(MyAnnotation.class).length == 2,
                "Array should contain 2 annotations");
        Asserts.assertEquals(new CustomAnnotations().getAnnotationsByType(MyAnnotation.class)[1].name(),
                "Bar", "Should be 'Bar'");
    }

    static class CustomAnnotations implements AnnotatedElement {
        @Override
        public Annotation[] getDeclaredAnnotations() {
            Annotation[] res = new Annotation[1];
            res[0] = new MyAnnotationsImpl();
            return res;
        }

        @Override
        public Annotation[] getAnnotations() {
            return getDeclaredAnnotations();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }
    }

    static class MyAnnotationsImpl implements MyAnnotations {
        public MyAnnotation[] value() {
            MyAnnotation[] res = new MyAnnotation[2];
            res[0] = new MyAnnotationImpl("Foo");
            res[1] = new MyAnnotationImpl("Bar");
            return res;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return MyAnnotations.class;
        }
    }

    static class MyAnnotationImpl implements MyAnnotation {
        private String val;
        MyAnnotationImpl(String val) {
            this.val = val;
        }

        public String name() { return val; }

        @Override
        public Class<? extends Annotation> annotationType() {
            return MyAnnotations.class;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnnotations {
        MyAnnotation[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(MyAnnotations.class)
    @interface MyAnnotation {
        String name();
    }
}
