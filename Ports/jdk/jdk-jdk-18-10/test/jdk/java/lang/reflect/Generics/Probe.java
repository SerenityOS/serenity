/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5003916 6704655 6873951 6476261 8004928
 * @summary Testing parsing of signatures attributes of nested classes
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import static java.util.Arrays.*;

@Classes({"java.util.concurrent.FutureTask",
          "java.util.concurrent.ConcurrentHashMap$EntryIterator",
          "java.util.concurrent.ConcurrentHashMap$KeyIterator",
          "java.util.concurrent.ConcurrentHashMap$ValueIterator",
          "java.util.AbstractList$ListItr",
          "java.util.EnumMap$EntryIterator",
          "java.util.EnumMap$KeyIterator",
          "java.util.EnumMap$ValueIterator",
          "java.util.IdentityHashMap$EntryIterator",
          "java.util.IdentityHashMap$KeyIterator",
          "java.util.IdentityHashMap$ValueIterator",
          "java.util.WeakHashMap$EntryIterator",
          "java.util.WeakHashMap$KeyIterator",
          "java.util.WeakHashMap$ValueIterator",
          "java.util.HashMap$EntryIterator",
          "java.util.HashMap$KeyIterator",
          "java.util.HashMap$ValueIterator",
          "java.util.LinkedHashMap$LinkedEntryIterator",
          "java.util.LinkedHashMap$LinkedKeyIterator",
          "java.util.LinkedHashMap$LinkedValueIterator"})
public class Probe {
    public static void main (String... args) throws Throwable {
        Classes classesAnnotation = (Probe.class).getAnnotation(Classes.class);
        List<String> names = new ArrayList<>(asList(classesAnnotation.value()));

        int errs = 0;
        for(String name: names) {
            System.out.println("\nCLASS " + name);
            Class c = Class.forName(name, false, null);
            errs += probe(c);
            System.out.println(errs == 0 ? "  ok" : "  ERRORS:" + errs);
        }

        if (errs > 0 )
            throw new RuntimeException("Errors during probing.");
    }

    static int probe (Class c) {
        int errs = 0;

        try {
            c.getTypeParameters();
            c.getGenericSuperclass();
            c.getGenericInterfaces();
        } catch (Throwable t) {
            errs++;
            System.err.println(t);
        }

        Field[] fields = c.getDeclaredFields();
        if (fields != null)
            for(Field field: fields) {
                try {
                    field.getGenericType();
                } catch (Throwable t) {
                    errs++;
                    System.err.println("FIELD " + field);
                    System.err.println(t);
                }
            }

        Method[] methods = c.getDeclaredMethods();
        if (methods != null)
            for(Method method: methods) {
                try {
                    method.getTypeParameters();
                    method.getGenericReturnType();
                    method.getGenericParameterTypes();
                    method.getGenericExceptionTypes();
                } catch (Throwable t) {
                    errs++;
                    System.err.println("METHOD " + method);
                    System.err.println(t);
                }
            }

        Constructor[] ctors = c.getDeclaredConstructors();
        if (ctors != null)
            for(Constructor ctor: ctors) {
                try {
                    ctor.getTypeParameters();
                    ctor.getGenericParameterTypes();
                    ctor.getGenericExceptionTypes();
                } catch (Throwable t) {
                    errs++;
                    System.err.println("CONSTRUCTOR " + ctor);
                    System.err.println(t);
                }
            }

        return errs;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Classes {
    String [] value(); // list of classes to probe
}
