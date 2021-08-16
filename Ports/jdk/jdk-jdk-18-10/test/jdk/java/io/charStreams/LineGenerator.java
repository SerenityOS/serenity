/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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

/**/


public class LineGenerator {

    IntGenerator ig;
    StringGenerator sg;
    int limit;

    public LineGenerator(IntGenerator ig, StringGenerator sg, int limit) {
        this.ig = ig;
        this.sg = sg;
        this.limit = limit;
    }

    public LineGenerator(IntGenerator ig) {
        this.ig = ig;
        this.sg = new StringGenerator(ig);
        this.limit = -1;
    }

    private char prevTerminator = 0;
    private int count = 0;
    public String lineTerminator;

    public String next() {
        if ((count >= limit) && (limit >= 0))
            return null;

        String l = sg.next();

        /* Avoid "\r\n" sequences
           in which the '\n' terminates a blank line */
        int len = l.length();
        int t;
        do
            t = ig.next(2);
        while ((prevTerminator == '\r') && (len == 0) && (t == 0));

        String ts;
        switch (t) {
        case 0:
            ts = "\n";
            prevTerminator = '\n';
            break;
        case 1:
            ts = "\r";
            prevTerminator = '\r';
            break;
        case 2:
        default:
            ts = "\r\n";
            prevTerminator = '\n';
            break;
        }

        count++;
        lineTerminator = ts;
        return l;
    }
}
