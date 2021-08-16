/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6844907
 * @modules java.security.jgss/sun.security.krb5.internal.crypto
 * @run main/othervm ETypeOrder
 * @summary krb5 etype order should be from strong to weak
 */

import sun.security.krb5.internal.crypto.EType;

public class ETypeOrder {
    public static void main(String[] args) throws Exception {

        // File does not exist, so that the system-default one won't be used
        System.setProperty("java.security.krb5.conf", "no_such_file");
        int[] etypes = EType.getBuiltInDefaults();

        // Reference order, note that 2 is not implemented in Java
        int correct[] = { 18, 17, 20, 19, 16, 23, 1, 3, 2 };

        int match = 0;
        loopi: for (int i=0; i<etypes.length; i++) {
            for (; match < correct.length; match++) {
                if (etypes[i] == correct[match]) {
                    System.out.println("Find " + etypes[i] + " at #" + match);
                    continue loopi;
                }
            }
            throw new Exception("No match or bad order for " + etypes[i]);
        }
    }
}
