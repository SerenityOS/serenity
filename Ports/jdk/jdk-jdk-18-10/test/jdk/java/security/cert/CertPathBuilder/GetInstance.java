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
 * @bug 4408997
 * @summary Should be able to pass a Provider object to getInstance().
 * @compile StubProvider.java StubProviderImpl.java
 * @run main/othervm/policy=provider.policy GetInstance
 * The test passes if it returns.
 * The test fails if an exception is thrown.
 */
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.Provider;
import java.security.AccessController;
import java.security.cert.CertPathBuilder;
import java.security.NoSuchAlgorithmException;


public class GetInstance {

    public static void main(String[] argv) throws Exception {
        Provider stubProvider = new StubProvider();
        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX", stubProvider);
        System.out.println("Test passed.");
    }
}
