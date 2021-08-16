/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6419926
 * @summary JSR 199: FileObject.toUri() generates URI without schema (Solaris)
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.tools.*;

public class T6419926 {
    public static void main(String[] argv) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager mgr = compiler.getStandardFileManager( new DiagnosticCollector<JavaFileObject>(), null, null)) {
            System.out.println( new File( new File(".").toURI() ).getAbsolutePath() );
            mgr.setLocation(StandardLocation.CLASS_OUTPUT,
                                Collections.singleton(new File(".")));

            FileObject fo = mgr.getFileForOutput(StandardLocation.CLASS_OUTPUT,
                                    "", "file.to.delete", null);
            URI uri = fo.toUri();
            System.out.println( uri );

            if (!"file".equals(uri.getScheme()))
                throw new Exception("unexpected scheme for uri: " + uri.getScheme());
        }
    }
}
