/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StringHttp {

    private byte[] httpRequest;
    private String[] httpResponse;
    private byte[] buf;

    @Setup
    public void setup() {
        buf = new byte[4080];
        httpRequest = "GET /foo/bar/baz HTTP/1.1\nHost: foo.com\n".getBytes();
        httpResponse = new String[]{"Date: 4/20/2003 10:21:31", "Last-Modified: 4/15/2003 10:21:31",
                "Content-Length: 1234", "", "foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz",
                "foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz",
                "foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz",
                "foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz",
                "foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz"};
    }

    @Benchmark
    public void parseRequest(Blackhole bh) {
        bh.consume(new String(httpRequest, 0, 3));
        bh.consume(new String(httpRequest, 5, 11));
        bh.consume(new String(httpRequest, 17, 8));
        bh.consume(new String(httpRequest, 32, 7));
    }

    @Benchmark
    public int bufferResponse() {
        int pos = 0;
        int n = httpResponse.length;

        for (String s : httpResponse) {
            byte[] b = s.getBytes();
            int len = b.length;

            System.arraycopy(b, 0, buf, pos, len);
            pos += len;
            buf[pos++] = '\n';
        }
        return n;
    }

}
