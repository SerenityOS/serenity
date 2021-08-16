/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import org.testng.annotations.Test;
import static org.testng.Assert.expectThrows;
import static org.testng.Assert.fail;

/*
 * @test
 * @bug 8251715
 * @summary This test verifies exception when resources for
 * SSLcontext used by HttpClient are not available
 * @build SSLExceptionTest
 * @run testng/othervm -Djdk.tls.client.protocols="InvalidTLSv1.4"
 *                      SSLExceptionTest
 */

public class SSLExceptionTest  {

    Throwable excp,noSuchAlgo;

    static final int ITERATIONS = 10;

    @Test
    public void testHttpClientsslException() {
        for (int i = 0; i < ITERATIONS; i++) {
            excp = expectThrows(UncheckedIOException.class, HttpClient.newBuilder()::build);
            noSuchAlgo = excp.getCause().getCause();
            if ( !(noSuchAlgo instanceof NoSuchAlgorithmException) ) {
                fail("Test failed due to wrong exception cause : " + noSuchAlgo);
            }
            excp = expectThrows(UncheckedIOException.class, HttpClient::newHttpClient);
            noSuchAlgo = excp.getCause().getCause();
            if ( !(noSuchAlgo instanceof NoSuchAlgorithmException) ) {
                fail("Test failed due to wrong exception cause : " + noSuchAlgo);
            }
        }
    }
}

