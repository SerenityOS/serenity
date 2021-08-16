/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023878
 * @summary Test that the right kind of exception is thrown from the type
 *          annotation reflection code.
 * @run testng BadCPIndex
 */

import java.lang.annotation.*;
import java.util.Base64;
import java.util.function.Function;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

public class BadCPIndex {
    private static final  MyLoader loader = new MyLoader(BadCPIndex.class.getClassLoader());

    // Blueprint for broken C
    //public static class C extends @BadCPIndex.A Object {}
    private static final String encodedBrokenC = "yv66vgAAADQAFgoAAwAPBwARBwATAQAGPGluaXQ+AQADKClWAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEAClNvdXJjZUZpbGUBAA9CYWRDUEluZGV4LmphdmEBAB1SdW50aW1lVmlzaWJsZVR5cGVBbm5vdGF0aW9ucwcAFAEAAUEBAAxJbm5lckNsYXNzZXMBAA5MQmFkQ1BJbmRleCRBOwwABAAFBwAVAQAMQmFkQ1BJbmRleCRDAQABQwEAEGphdmEvbGFuZy9PYmplY3QBAAxCYWRDUEluZGV4JEEBAApCYWRDUEluZGV4ACEAAgADAAAAAAABAAEABAAFAAEABgAAAB0AAQABAAAABSq3AAGxAAAAAQAHAAAABgABAAAAKQADAAgAAAACAAkACgAAAAoAARD//wAADwAAAA0AAAASAAIACwAQAAwmCQACABAAEgAJ";

    // Blueprint for broken D
    //public static class D<@BadCPIndex.B U> {}
    private static final String encodedBrokenD = "yv66vgAAADQAGAoAAwARBwATBwAVAQAGPGluaXQ+AQADKClWAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEACVNpZ25hdHVyZQEAKDxVOkxqYXZhL2xhbmcvT2JqZWN0Oz5MamF2YS9sYW5nL09iamVjdDsBAApTb3VyY2VGaWxlAQAPQmFkQ1BJbmRleC5qYXZhAQAdUnVudGltZVZpc2libGVUeXBlQW5ub3RhdGlvbnMHABYBAAFCAQAMSW5uZXJDbGFzc2VzAQAOTEJhZENQSW5kZXgkQjsMAAQABQcAFwEADEJhZENQSW5kZXgkRAEAAUQBABBqYXZhL2xhbmcvT2JqZWN0AQAMQmFkQ1BJbmRleCRCAQAKQmFkQ1BJbmRleAAhAAIAAwAAAAAAAQABAAQABQABAAYAAAAdAAEAAQAAAAUqtwABsQAAAAEABwAAAAYAAQAAAEAABAAIAAAAAgAJAAoAAAACAAsADAAAAAkAAQAAAAARAAAADwAAABIAAgANABIADiYJAAIAEgAUAAk=";

    // Blueprint for broken E
    //public static class E extends @BadCPIndex.A Object {}
    private static final String encodedBrokenE = "yv66vgAAADQAFgoAAwAPBwARBwATAQAGPGluaXQ+AQADKClWAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEAClNvdXJjZUZpbGUBAA9CYWRDUEluZGV4LmphdmEBAB1SdW50aW1lVmlzaWJsZVR5cGVBbm5vdGF0aW9ucwcAFAEAAUEBAAxJbm5lckNsYXNzZXMBAA5MQmFkQ1BJbmRleCRBOwwABAAFBwAVAQAMQmFkQ1BJbmRleCRFAQABRQEAEGphdmEvbGFuZy9PYmplY3QBAAxCYWRDUEluZGV4JEEBAApCYWRDUEluZGV4ACEAAgADAAAAAAABAAEABAAFAAEABgAAAB0AAQABAAAABSq3AAGxAAAAAQAHAAAABgABAAAARgADAAgAAAACAAkACgAAAAoAARD//wAADgAKAA0AAAASAAIACwAQAAwmCQACABAAEgAJ";

    private static final Object[][] cases = {
        { new Case("BadCPIndex$C", encodedBrokenC, Class::getAnnotatedSuperclass) },
        { new Case("BadCPIndex$D", encodedBrokenD, (c -> c.getTypeParameters()[0].getAnnotations()))},
        { new Case("BadCPIndex$E", encodedBrokenE, Class::getAnnotatedSuperclass) },
    };

    @DataProvider
    public static Object[][] data() { return cases; }

    @Test(dataProvider="data")
    public static void testOpThrowsAFE(Case testCase) {
        Class<?> c = loader.defineClass(testCase.name, Base64.getDecoder().decode(testCase.encoding));
        try {
            System.out.println("Testing: " + c);
            testCase.trigger.apply(c);
            throw new RuntimeException("Expecting AnnotationFormatError here");
        } catch (AnnotationFormatError e) {
            ; //ok
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MyLoader extends ClassLoader {
        public MyLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static class Case {
        public String name;
        public String encoding;
        public Function<Class<?>, Object> trigger;

        public Case(String name, String encoding, Function<Class<?>, Object> trigger) {
            this.name = name;
            this.encoding = encoding;
            this.trigger = trigger;
        }
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface A {}

    @Target(ElementType.TYPE_PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface B {}
}
