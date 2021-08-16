/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007320 8014709 8163231
 * @summary Test all optional fields in ConstMethod
 * @compile -g -parameters ConstMethodTest.java
 * @run main ConstMethodTest
 */

import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.io.Serializable;

@Retention(RetentionPolicy.RUNTIME)
@interface MyAnnotation {
    public String name();
    public String value();
    public String date() default "today";
}

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@interface TypeAnno {
    String value();
}

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@interface TypeAnno2 {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface Named {
  String value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface ScalarTypesWithDefault {
    byte     b()    default 11;
    short    s()    default 12;
    int      i()    default 13;
    long     l()    default 14;
    char     c()    default 'V';
}

// Some exception class
class OkException extends RuntimeException {};


@MyAnnotation(name="someName", value = "Hello World")
public class ConstMethodTest {
    public @TypeAnno("constructor") ConstMethodTest() { }

    public ConstMethodTest(int i) {
        // needs a second unannotated constructor
    }

    private static void check(boolean b) {
        if (!b)
            throw new RuntimeException();
    }
    private static void fail(String msg) {
       System.err.println(msg);
       throw new RuntimeException();
    }
    private static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) {
        } else {
            fail(x + " not equal to " + y);
        }
    }
    private static final String[] parameter_names = {
        "parameter", "parameter2", "x"
    };

    // Declare a function with everything in it.
    @MyAnnotation(name="someName", value="Hello World")
    static <T> void kitchenSinkFunc(@Named(value="aName") String parameter,
              @Named("bName") String parameter2,
              @ScalarTypesWithDefault T x)
            throws @TypeAnno("RE") @TypeAnno2("RE2") RuntimeException,
                NullPointerException,
                @TypeAnno("AIOOBE") ArrayIndexOutOfBoundsException {
      int i, j, k;
      try {
        System.out.println("calling kitchenSinkFunc " + parameter);
        throw new OkException();  // to see stack trace with line numbers
      } catch (Exception e) {
       e.printStackTrace();
      }
    }

    private static void test1() throws Throwable {
        for (Method m : ConstMethodTest.class.getDeclaredMethods()) {
            if (m.getName().equals("kitchenSinkFunc")) {
                Annotation[][] ann = m.getParameterAnnotations();
                equal(ann.length, 3);
                Annotation foo = ann[0][0];
                Annotation bar = ann[1][0];
                equal(foo.toString(), "@Named(\"aName\")");
                equal(bar.toString(), "@Named(\"bName\")");
                check(foo.equals(foo));
                check(bar.equals(bar));
                check(! foo.equals(bar));
                // method annotations
                Annotation[] ann2 = m.getAnnotations();
                equal(ann2.length, 1);
                Annotation mann = ann2[0];
                equal(mann.toString(), "@MyAnnotation(date=\"today\", name=\"someName\", value=\"Hello World\")");
                // Test Method parameter names
                Parameter[] parameters = m.getParameters();
                if(parameters == null)
                    throw new Exception("getParameters should never be null");
                for(int i = 0; i < parameters.length; i++) {
                    Parameter p = parameters[i];
                    equal(parameters[i].getName(), parameter_names[i]);
                }
            }
        }
    }

    private static void testConstructor() throws Exception {
        for (Constructor c : ConstMethodTest.class.getDeclaredConstructors()) {
            Annotation[] aa = c.getAnnotatedReturnType().getAnnotations();
            if (c.getParameterTypes().length == 1) { // should be un-annotated
                check(aa.length == 0);
            } else if (c.getParameterTypes().length == 0) { //should be annotated
                check(aa.length == 1);
                check(((TypeAnno)aa[0]).value().equals("constructor"));
            } else {
                //should not happen
                check(false);
            }
        }
    }

    public static void main(java.lang.String[] unused) throws Throwable {
        // pass 5 so kitchenSinkFunc is instantiated with an int
        kitchenSinkFunc("parameter", "param2", 5);
        test1();
        testConstructor();
    }
};

