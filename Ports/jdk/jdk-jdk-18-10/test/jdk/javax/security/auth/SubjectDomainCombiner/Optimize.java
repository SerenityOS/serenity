/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4887017
 * @summary SubjectDomainCombiner optimization incorrect
 */

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;
import java.security.*;

public class Optimize {

    public static void main(String[] args) {

        ProtectionDomain pd1 = new ProtectionDomain(
            new CodeSource(null, (java.security.cert.Certificate[]) null),
            new Permissions(),
            null, null);
        ProtectionDomain pd2 = new ProtectionDomain(
            new CodeSource(null, (java.security.cert.Certificate[]) null),
            new Permissions(),
            null, null);
        ProtectionDomain pd3 = new ProtectionDomain(
            new CodeSource(null, (java.security.cert.Certificate[]) null),
            new Permissions(),
            null, null);

        ProtectionDomain[] current = new ProtectionDomain[] {pd1, pd2};
        ProtectionDomain[] assigned = new ProtectionDomain[] {pd3, pd2};

        SubjectDomainCombiner sdc = new SubjectDomainCombiner(new Subject());
        ProtectionDomain[] combined = sdc.combine(current, assigned);

        // this depends on current SubjectDomainCombiner implementation
        // (ordering of returned domains)
        if (combined.length == 4 &&
            combined[0] != pd1 && combined[1] != pd2 &&
            combined[2] == pd3 && combined[3] == pd2) {
            System.out.println("test passed");
        } else {
            System.out.println("test failed");
            throw new SecurityException("Test Failed");
        }
    }
}
