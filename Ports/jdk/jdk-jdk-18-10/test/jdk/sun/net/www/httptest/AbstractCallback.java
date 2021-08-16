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

import java.net.*;
import java.util.*;
import java.io.IOException;

/**
 * This class provides a partial implementation of the HttpCallback
 * interface. Use this class if you want to use the requestURI as a means
 * of tracking multiple invocations of a request (on the server).
 * In this case, you implement the modified request() method, which includes
 * an integer count parameter. This parameter indicates the number of times
 * (starting at zero) the request URI has been received.
 */

public abstract class AbstractCallback implements HttpCallback {

    Map requests;

    static class Request {
        URI uri;
        int count;

        Request (URI u) {
            uri = u;
            count = 0;
        }
    }

    AbstractCallback () {
        requests = Collections.synchronizedMap (new HashMap());
    }

    /**
     * handle the given request and generate an appropriate response.
     * @param msg the transaction containing the request from the
     *        client and used to send the response
     */
    public void request (HttpTransaction msg) {
        URI uri = msg.getRequestURI();
        Request req = (Request) requests.get (uri);
        if (req == null) {
            req = new Request (uri);
            requests.put (uri, req);
        }
        request (msg, req.count++);
    }

    /**
     * Same as HttpCallback interface except that the integer n
     * is provided to indicate sequencing of repeated requests using
     * the same request URI. n starts at zero and is incremented
     * for each successive call.
     *
     * @param msg the transaction containing the request from the
     *        client and used to send the response
     * @param n value is 0 at first call, and is incremented by 1 for
     *        each subsequent call using the same request URI.
     */
    abstract public void request (HttpTransaction msg, int n);
}
