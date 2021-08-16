/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4666195
 * @build getResponseCode
 * @run main getResponseCode
 * @summary REGRESSION: HttpURLConnection.getResponseCode() returns always -1
*/
import java.net.*;
import java.io.*;

public class getResponseCode {
    public static void main(String[] args) throws Exception {
        try {
            MyHttpURLConnectionImpl myCon = new MyHttpURLConnectionImpl(null);
            int responseCode = myCon.getResponseCode();
            if (responseCode == -1) {
                throw new RuntimeException("java.net.HttpURLConnection "
                                           +"should provide implementation "
                                           +"for getResponseCode()");
            }
        } catch (java.net.UnknownServiceException e) {
            System.out.println("PASS");
        }
    }
}

class MyHttpURLConnectionImpl extends java.net.HttpURLConnection {
    MyHttpURLConnectionImpl(URL url) {
        super(url);
    }

    public boolean usingProxy(){
        return true;
    }

    public void connect(){
    }

    public void disconnect(){
    }
}
