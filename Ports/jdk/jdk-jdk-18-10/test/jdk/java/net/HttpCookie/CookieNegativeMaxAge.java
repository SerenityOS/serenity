/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005068
 * @summary Check that any negative maxAge is treated as "unspecified" and
 * if header contains cookie with "expires" attribute in the past then cookie
 * is created with maxAge=0 meaning it is specified to be immediately expired.
 * @run main CookieNegativeMaxAge
 */


import java.net.HttpCookie;
import java.util.List;

public class CookieNegativeMaxAge {

    public static void main(String... args) {
        HttpCookie cookie = new HttpCookie("testCookie", "value");
        cookie.setMaxAge(Integer.MIN_VALUE);
        if (cookie.hasExpired()) {
            throw new RuntimeException("Cookie has unexpectedly expired");
        }

        List<HttpCookie> cookies = HttpCookie.parse("Set-Cookie: " +
                "expiredCookie=value; expires=Thu, 01 Jan 1970 00:00:00 GMT");
        if (cookies.size() == 1) {
            if (cookies.get(0).getMaxAge() != 0) {
                throw new RuntimeException("Cookie maxAge expected to be 0");
            }
        } else {
            throw new RuntimeException("Header was incorrectly parsed");
        }
    }
}
