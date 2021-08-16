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
 * @run main NoName
 * @summary The reflection API should report parameters with no name properly.
 */
import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.MalformedParametersException;
import java.lang.ClassLoader;
import java.lang.ClassNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class NoName {

    private static final byte[] NoName_bytes = {
        -54,-2,-70,-66,0,0,0,52,
        0,18,10,0,3,0,15,7,
        0,16,7,0,17,1,0,6,
        60,105,110,105,116,62,1,0,
        3,40,41,86,1,0,4,67,
        111,100,101,1,0,15,76,105,
        110,101,78,117,109,98,101,114,
        84,97,98,108,101,1,0,1,
        109,1,0,5,40,73,73,41,
        86,1,0,16,77,101,116,104,
        111,100,80,97,114,97,109,101,
        116,101,114,115,1,0,0,1,
        0,1,98,1,0,10,83,111,
        117,114,99,101,70,105,108,101,
        1,0,14,69,109,112,116,121,
        78,97,109,101,46,106,97,118,
        97,12,0,4,0,5,1,0,
        9,69,109,112,116,121,78,97,
        109,101,1,0,16,106,97,118,
        97,47,108,97,110,103,47,79,
        98,106,101,99,116,0,33,0,
        2,0,3,0,0,0,0,0,
        2,0,1,0,4,0,5,0,
        1,0,6,0,0,0,29,0,
        1,0,1,0,0,0,5,42,
        -73,0,1,-79,0,0,0,1,
        0,7,0,0,0,6,0,1,
        0,0,0,1,0,1,0,8,
        0,9,0,2,0,6,0,0,
        0,25,0,0,0,3,0,0,
        0,1,-79,0,0,0,1,0,
        7,0,0,0,6,0,1,0,
        0,0,2,
        // Method Parameters attribute here
        0,10,
        // attribute_length
        0,0,0,9,
        // parameter_count
        2,
        // first parameter name
        0,0,
        // first parameter modifiers
        0,0,
        // second parameter name
        0,12,
        // second parameter modifiers
        0,0,
        // end attribute
        0,1,0,13,0,0,0,2,0,14
    };

    private static class InMemoryClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    };

    private static final InMemoryClassLoader loader = new InMemoryClassLoader();

    private final Class<?> noName;

    private NoName() throws ClassNotFoundException {
        noName = loader.defineClass("EmptyName", NoName_bytes);
    }

    public static void main(String... args)
        throws NoSuchMethodException, IOException, ClassNotFoundException {
        new NoName().run();
    }

    public void run() throws NoSuchMethodException {
        final Class<?> cls = noName;
        System.err.println("Trying " + cls);
        final Method method = cls.getMethod("m", int.class, int.class);
        final Parameter[] params = method.getParameters();
        System.err.println("Name " + params[0].getName());
        System.err.println("Name " + params[1].getName());
    }

}
