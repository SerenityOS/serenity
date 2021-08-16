/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * Added 5054915:  SSLv2 ciphersuite names not in export list.
 * to this test, rather than creating a brand new test.
 */

import java.util.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class CipherSuites {
    public static final String[] STANDARD = {
        "SSL_RSA_WITH_RC4_128_MD5",
        "SSL_RSA_WITH_RC4_128_SHA",
        "SSL_CK_RC4_128_WITH_MD5",
        "SSL_CK_RC4_128_EXPORT40_WITH_MD5",
        "SSL_CK_RC2_128_CBC_WITH_MD5",
        "SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5",
        "SSL_CK_IDEA_128_CBC_WITH_MD5",
        "SSL_CK_DES_64_CBC_WITH_MD5",
        "SSL_CK_DES_192_EDE3_CBC_WITH_MD5"
    };
    public static final String[] CUSTOM = {
        "SSL_RSA_WITH_RC4_128_MD5",
        "SSL_RSA_WITH_RC4_128_SHA",
        "TLS_RSA_WITH_RC4_1024_SHA",    // <--- custom ciphersuite not in old export list
        "SSL_CK_RC4_128_WITH_MD5",
        "SSL_CK_RC4_128_EXPORT40_WITH_MD5",
        "SSL_CK_RC2_128_CBC_WITH_MD5",
        "SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5",
        "SSL_CK_IDEA_128_CBC_WITH_MD5",
        "SSL_CK_DES_64_CBC_WITH_MD5",
        "SSL_CK_DES_192_EDE3_CBC_WITH_MD5"
    };
}
