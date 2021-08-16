/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6791927 8233886
 * @summary Wrong Locale in HttpCookie::expiryDate2DeltaSeconds
 * @run main/othervm B6791927
 */

import java.net.*;
import java.util.List;
import java.util.Locale;

public class B6791927 {
    public static final void main(String[] aaParamters) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        try {
            // Forces a non US locale
            Locale.setDefault(Locale.FRANCE);
            List<HttpCookie> cookies = HttpCookie.parse("set-cookie:" +
                    " CUSTOMER=WILE_E_COYOTE;" +
                    " expires=Sat, 09-Nov-2041 23:12:40 GMT");
            if (cookies == null || cookies.isEmpty()) {
                throw new RuntimeException("No cookie found");
            }
            for (HttpCookie c : cookies) {
                if (c.getMaxAge() == 0) {
                    throw new RuntimeException(
                        "Expiration date shouldn't be 0");
                }
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }
}
