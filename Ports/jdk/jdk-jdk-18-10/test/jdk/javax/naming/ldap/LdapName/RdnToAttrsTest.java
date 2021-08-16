/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6245035
 * @summary [J2SE 1.5] Rdn generates incorrect attributes sometimes
 */

import javax.naming.ldap.*;
import javax.naming.*;
import java.util.*;
import javax.naming.directory.*;
import java.io.*;

public class RdnToAttrsTest {

    public static void main(String args[])
                throws Exception {

    Rdn rdn = new Rdn("cn = commonName1 + cn = commonName2");
    String attrStr = rdn.toAttributes().toString();
    System.out.println("attributes=" + attrStr);
    if ("{cn=cn: commonName1, commonName2}".equals(attrStr)) {
        System.out.println("The test PASSED");
    } else {
        throw new Exception("The test failed");
    }
}
}
