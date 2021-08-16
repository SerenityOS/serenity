/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8230159
 * @library /test/lib
 * @summary Ensure that correct exceptions are being thrown in
 * BasicAuthenticator constructor
 * @run testng BasicAuthenticatorExceptionCheck
 */


import java.nio.charset.Charset;
import com.sun.net.httpserver.BasicAuthenticator;
import org.testng.annotations.Test;

import static org.testng.Assert.expectThrows;
import static java.nio.charset.StandardCharsets.UTF_8;


public class BasicAuthenticatorExceptionCheck {
    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    static BasicAuthenticator createBasicAuthenticator(String realm, Charset charset) {
        return new BasicAuthenticator(realm, charset) {
            public boolean checkCredentials(String username, String pw) {
                return true;
            }
        };
    }
    static BasicAuthenticator createBasicAuthenticator(String realm) {
        return new BasicAuthenticator(realm) {
            public boolean checkCredentials(String username, String pw) {
                return true;
            }
        };
    }

    @Test
    public void testAuthenticationException() {

        Throwable ex = expectThrows(NPE, () ->
                createBasicAuthenticator("/test", null));
        System.out.println("Valid realm and Null charset provided - " +
                "NullPointerException thrown as expected: " + ex);

        ex = expectThrows(NPE, () ->
                createBasicAuthenticator(null, UTF_8));
        System.out.println("Null realm and valid charset provided - " +
                "NullPointerException thrown as expected: " + ex);

        ex = expectThrows(IAE, () ->
                createBasicAuthenticator("", UTF_8));
        System.out.println("Empty string for realm and valid charset provided - " +
                "IllegalArgumentException thrown as expected: " + ex);

        ex = expectThrows(NPE, () ->
                createBasicAuthenticator(null));
        System.out.println("Null realm provided - " +
                "NullPointerException thrown as expected: " + ex);

        ex = expectThrows(IAE, () ->
                createBasicAuthenticator(""));
        System.out.println("Empty string for realm provided - " +
                "IllegalArgumentException thrown as expected: " + ex);
    }
}
