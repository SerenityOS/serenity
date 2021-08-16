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
 * @bug 4417268
 * @summary Update HttpsURLConnection to not call getDefault in initializer.
 * @author Brad Wetmore
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

public class DelayDefaultContextLoading {

    /*
     * Pick some static method in HttpsURLConnection, and
     * then run it.  If we're still broken, we'll call
     * SSLSocketFactory.getDefault(), which will create a
     * new SecureRandom number generator, which takes a while
     * to load.
     */
    public static void main(String[] args) throws Exception {
        Date date1 = new Date();
        HttpsURLConnection.getDefaultHostnameVerifier();
        Date date2 = new Date();
        long delta = (date2.getTime() - date1.getTime()) / 1000;

        /*
         * Did it take longer than 5 second to run?
         * If so, we're probably still loading incorrectly.
         */
        if (delta > 5) {
            throw new Exception("FAILED:  HttpsURLConnection took " + delta +
                " seconds to load");
        }
        System.out.println("PASSED:  HttpsURLConnection took " + delta +
            " seconds to load");
    }
}
