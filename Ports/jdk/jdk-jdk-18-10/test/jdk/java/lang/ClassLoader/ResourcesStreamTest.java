/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * @test
 * @bug 8161230
 * @summary Test java.lang.ClassLoader.resources() method
 *
 * @build ResourcesStreamTest
 * @run main ResourcesStreamTest
 */
public class ResourcesStreamTest {

    public static void main(String[] args) throws Exception {
        testSuccess();
        testFailure();
    }

    public static void testSuccess() throws Exception {
        // failing part first
        try {
            ClassLoader cl = new FailingClassLoader();
            // should create the stream pipe
            Stream<URL> stream = cl.resources("the name");
            // expect function to throw an exception when calling the method
            stream.forEach(System.out::println);
            throw new Exception("expected UncheckedIOException not thrown");
        } catch (UncheckedIOException uio) {
            String causeMessage = uio.getCause().getMessage();
            if (!"the name".equals(causeMessage))
                throw new Exception("unexpected cause message: " + causeMessage);
        }
    }

    public static void testFailure() throws Exception {
        ClassLoader cl = new SuccessClassLoader();
        long count = cl.resources("the name").count();
        if (count != 1)
            throw new Exception("expected resource is null or empty");

        cl.resources("the name")
          .filter(url -> "file:/somefile".equals(url.toExternalForm()))
          .findFirst()
          .orElseThrow(() -> new Exception("correct URL not found"));
    }

    public static class SuccessClassLoader extends ClassLoader {
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            URL url = new URL("file:/somefile");
            return Collections.enumeration(Collections.singleton(url));
        }
    }

    public static class FailingClassLoader extends ClassLoader {
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            throw new IOException(name);
        }
    }
}
