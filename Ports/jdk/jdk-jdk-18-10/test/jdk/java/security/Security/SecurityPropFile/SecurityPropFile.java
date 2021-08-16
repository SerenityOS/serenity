/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4303068
 * @summary be allowed to specify the security properties file
 *      as a -D system property
 *
 * @run main/othervm/policy=SecurityPropFile.policy -Djava.security.properties=${test.src}/SecurityPropFile.file -Djava.security.debug=properties SecurityPropFile
 */

public class SecurityPropFile {
    public static void main(String[] args) {
        System.out.println(java.security.Security.getProperty
                                ("policy.provider"));
        System.out.println(java.security.Security.getProperty
                                ("policy.url.1"));
        System.out.println(java.security.Security.getProperty
                                ("policy.url.2"));
    }
}
