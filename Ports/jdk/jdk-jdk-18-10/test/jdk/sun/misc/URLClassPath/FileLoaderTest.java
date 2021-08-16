/*
 * Copyright (c) 2002, 2006, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4291009
 * @summary URLConnection fails to find resources
 *          when given file:/dir/./subdir/ URL
 */
import java.io.*;
import java.net.*;

public class FileLoaderTest {
  public static void main (String args[]) throws Exception {
      File tempFile = File.createTempFile("foo", ".txt");
      tempFile.deleteOnExit();
      String basestr = tempFile.toURL().toString();
      basestr = basestr.substring(0, basestr.lastIndexOf("/")+1);
      URL url = new URL(basestr+"."+"/");

      ClassLoader cl = new URLClassLoader (new URL[] { url });
      if (cl.getResource (tempFile.getName()) == null) {
          throw new RuntimeException("Returned null instead of " +
                                     tempFile.toURL().toString());
      }
   }
}
