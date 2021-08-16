/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests what happens when request publishers
 *          throw unexpected exceptions.
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters
 *        ReferenceTracker AbstractThrowingPublishers ThrowingPublishersInSubscribe
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.internal.httpclient.debug=true
 *                     -Djdk.httpclient.enableAllMethodRetry=true
 *                     ThrowingPublishersInSubscribe
 */

import org.testng.annotations.Test;

import java.util.Set;

public class ThrowingPublishersInSubscribe extends AbstractThrowingPublishers {

    @Test(dataProvider = "subscribeProvider")
    public void testThrowingAsString(String uri, boolean sameClient,
                                            Thrower thrower, Set<Where> whereValues)
            throws Exception
    {
        super.testThrowingAsStringImpl(uri, sameClient, thrower, whereValues);
    }
}
