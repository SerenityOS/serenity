/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8078891
 * @summary Ensure close() closes all component streams
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Close {

    public static void main(String[] argv) {
        byte[] buf = new byte[42];

        List<CloseableBAOS> list = List.of(new CloseableBAOS(buf),
            new CloseableBAOS(buf), new CloseableBAOS(buf),
            new CloseableBAOS(buf));

        Enumeration<CloseableBAOS> enumeration = Collections.enumeration(list);

        SequenceInputStream sequence = new SequenceInputStream(enumeration);
        try {
            sequence.close();
            throw new RuntimeException("Expected IOException not thrown");
        } catch (IOException e) {
            for (CloseableBAOS c : list) {
                if (!c.isClosed()) {
                    throw new RuntimeException("Component stream not closed");
                }
            }
            Throwable[] suppressed = e.getSuppressed();
            if (suppressed == null) {
                throw new RuntimeException("No suppressed exceptions");
            } else if (suppressed.length != list.size() - 1) {
                throw new RuntimeException("Expected " + (list.size() - 1) +
                    " suppressed exceptions but got " + suppressed.length);
            }
            for (Throwable t : suppressed) {
                if (!(t instanceof IOException)) {
                    throw new RuntimeException("Expected IOException but got " +
                        t.getClass().getName());
                }
            }
        }
    }

    static class CloseableBAOS extends ByteArrayInputStream{
        private boolean closed;

        CloseableBAOS(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            throw new IOException();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
