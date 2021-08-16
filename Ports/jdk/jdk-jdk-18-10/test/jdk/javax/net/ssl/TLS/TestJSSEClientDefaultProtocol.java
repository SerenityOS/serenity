/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049429 8234723
 * @modules java.management
 *          jdk.crypto.ec/sun.security.ec
 * @summary Test that all cipher suites work in all versions and all client
 *          authentication types. The way this is setup the server is stateless
 *          and all checking is done on the client side.
 * @compile CipherTestUtils.java JSSEClient.java JSSEServer.java
 * @run main/othervm
 *              -DSERVER_PROTOCOL=SSLv2Hello,SSLv3,TLSv1
 *              -DCLIENT_PROTOCOL=DEFAULT
 *              -DCIPHER=TLS_RSA_WITH_AES_128_CBC_SHA
 *          TestJSSE
 * @run main/othervm
 *              -DSERVER_PROTOCOL=SSLv2Hello,SSLv3,TLSv1,TLSv1.1,TLSv1.2
 *              -DCLIENT_PROTOCOL=DEFAULT
 *              -DCIPHER=TLS_RSA_WITH_AES_128_GCM_SHA256
 *          TestJSSE
 * @run main/othervm
 *              -DSERVER_PROTOCOL=SSLv2Hello,SSLv3,TLSv1,TLSv1.1,TLSv1.2
 *              -DCLIENT_PROTOCOL=DEFAULT
 *              -Djdk.tls.client.protocols=TLSv1
 *              -DCIPHER=TLS_RSA_WITH_AES_128_CBC_SHA
 *          TestJSSE
 * @run main/othervm
 *              -DSERVER_PROTOCOL=SSLv2Hello,SSLv3,TLSv1
 *              -DCLIENT_PROTOCOL=DEFAULT
 *              -Djdk.tls.client.protocols=TLSv1.2
 *              -DCIPHER=TLS_RSA_WITH_AES_128_GCM_SHA256
 *          TestJSSE javax.net.ssl.SSLHandshakeException
 * @run main/othervm
 *              -DSERVER_PROTOCOL=SSLv2Hello,SSLv3,TLSv1,TLSv1.1,TLSv1.2,TLSv1.3
 *              -DCLIENT_PROTOCOL=DEFAULT
 *              -Djdk.tls.client.protocols=TLSv1.3
 *              -DCIPHER=TLS_AES_256_GCM_SHA384
 *          TestJSSE
 * @run main/othervm
 *              -DSERVER_PROTOCOL=SSLv2Hello,SSLv3,TLSv1
 *              -DCLIENT_PROTOCOL=DEFAULT
 *              -Djdk.tls.client.protocols=TLSv1.3
 *              -DCIPHER=TLS_AES_256_GCM_SHA384
 *          TestJSSE javax.net.ssl.SSLHandshakeException
 */
