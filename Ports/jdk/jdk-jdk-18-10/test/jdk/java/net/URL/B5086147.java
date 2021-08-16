/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 5086147
 * @requires (os.family == "windows")
 * @run main B5086147
 * @summary File,URI,URL conversions are strange for UNC path
 */

import java.io.File;
import java.net.URI;

public class B5086147 {
    public static final void main( String[] aaParamters ) throws Exception{
        File file = new File( "\\\\somehost\\someshare\\somefile.ext" );
        URI uri = file.toURI();

        if (!(uri.toURL().toURI().equals(uri))) {
            throw new RuntimeException("Test failed : (uri.toURL().toURI().equals(uri)) isn't hold");
        }
    }
}
