/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.net.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import com.sun.net.httpserver.*;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;

class LogFilter extends Filter {

    PrintStream ps;
    DateFormat df;

    LogFilter (File file) throws IOException {
        ps = new PrintStream (new FileOutputStream (file));
        df = DateFormat.getDateTimeInstance();
    }

    /**
     * The filter's implementation, which is invoked by the serve r
     */
    public void doFilter (HttpExchange t, Filter.Chain chain) throws IOException
    {
        chain.doFilter (t);
        HttpContext context = t.getHttpContext();
        Headers rmap = t.getRequestHeaders();
        String s = df.format (new Date());
        s = s +" " + t.getRequestMethod() + " " + t.getRequestURI() + " ";
        s = s +" " + t.getResponseCode () +" " + t.getRemoteAddress();
        ps.println (s);
    }

    public void init (HttpContext ctx) {}

    public String description () {
        return "Request logger";
    }

    public void destroy (HttpContext c){}
}
