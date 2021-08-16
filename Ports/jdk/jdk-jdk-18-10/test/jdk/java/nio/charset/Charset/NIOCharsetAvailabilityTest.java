/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4777124 6920545 6911753 8073924
 * @summary Verify that all Charset subclasses are available through the API
 * @modules jdk.charsets
 */

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NIOCharsetAvailabilityTest {

    public static void main(String[] args) throws Exception {

        // build the set of all Charset subclasses in the
        // two known charset implementation packages
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Set<Class> charsets =
            Stream.concat(Files.walk(fs.getPath("/modules/java.base/sun/nio/cs/")),
                          Files.walk(fs.getPath("/modules/jdk.charsets/sun/nio/cs/ext/")))
                 .map( p -> p.subpath(2, p.getNameCount()).toString())
                 .filter( s ->  s.indexOf("$") == -1 && s.endsWith(".class"))
                 .map( s -> {
                     try {
                         return Class.forName(s.substring(0, s.length() - 6)
                                               .replace('/', '.'));
                     } catch (Exception x) {
                         throw new RuntimeException(x);
                     }
                  })
                 .filter( clz -> {
                     Class superclazz = clz.getSuperclass();
                     while (superclazz != null && !superclazz.equals(Object.class)) {
                         if (superclazz.equals(Charset.class)) {
                             return true;
                         } else {
                             superclazz = superclazz.getSuperclass();
                         }
                     }
                     return false;
                  })
                 .collect(Collectors.toCollection(HashSet::new));
        // remove the charsets that the API says are available
        Charset.availableCharsets()
               .values()
               .stream()
               .forEach(cs -> {
                   if (!charsets.contains(cs.getClass())) {
                       System.out.println(" missing -> " + cs.getClass());
                   }
                   charsets.remove(cs.getClass());
                });

        // remove the known pseudo-charsets that serve only to implement
        // other charsets, but shouldn't be known to the public
        charsets.remove(Class.forName("sun.nio.cs.Unicode"));
        charsets.remove(Class.forName("sun.nio.cs.ext.ISO2022"));
        charsets.remove(Class.forName("sun.nio.cs.ext.ISO2022_CN_GB"));
        charsets.remove(Class.forName("sun.nio.cs.ext.ISO2022_CN_CNS"));
        charsets.remove(Class.forName("sun.nio.cs.ext.JIS_X_0208_MS932"));
        charsets.remove(Class.forName("sun.nio.cs.ext.JIS_X_0212_MS5022X"));
        charsets.remove(Class.forName("sun.nio.cs.ext.JIS_X_0208_MS5022X"));
        try {
            charsets.remove(Class.forName("sun.nio.cs.ext.JIS_X_0208_Solaris"));
            charsets.remove(Class.forName("sun.nio.cs.ext.JIS_X_0212_Solaris"));
        } catch (ClassNotFoundException x) {
            // these two might be moved into stdcs
            charsets.remove(Class.forName("sun.nio.cs.JIS_X_0208_Solaris"));
            charsets.remove(Class.forName("sun.nio.cs.JIS_X_0212_Solaris"));
        }
        // report the charsets that are implemented but not available
        if (charsets.size() > 0) {
            charsets.stream()
                    .forEach( clz ->
                        System.out.println("Unused Charset subclass: " + clz));
            throw new RuntimeException();
        }
    }
}
