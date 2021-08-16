/*
 * Copyright (c) 1998, 2000, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 4086109
 *
 * @summary this tests support for the ftp URL syntax that includes a
 * user and password.
 * @author Benjamin Renaud
 */

/*
 *    ftp://<userid>:<password>@<host>/
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

public class UserAndPasswordTest {

    static String[] specs = {
        "ftp://springbank.eng/",
        "ftp://springbank.eng:21/",
        "ftp://userid:password@springbank.eng/",
        "ftp://userid:password@springbank.eng:21/",
        "ftp://userid:password@springbank.eng:90"
    };

    public static void main(String[] args) throws Exception {
        for (int i=0; i<specs.length; i++) {
            URL ftp = new URL(specs[i]);
            if (!"springbank.eng".equals(ftp.getHost())) {
                throw new Exception("Parsing of URLs with : broken");
            }
        }
    }

}
