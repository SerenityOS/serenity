/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4503092 8024883
 * @summary Tests that Windows Selector can use more than 63 channels
 * @run main LotsOfChannels
 * @run main/othervm -Dsun.nio.ch.maxUpdateArraySize=64 LotsOfChannels
 * @author kladko
 */

/* @test
 * @requires (os.family == "windows")
 * @run main/othervm -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider LotsOfChannels
 */

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class LotsOfChannels {

    private final static int PIPES_COUNT = 256;
    private final static int BUF_SIZE = 8192;
    private final static int LOOPS = 10;

    public static void main(String[] argv) throws Exception {
        Pipe[] pipes = new Pipe[PIPES_COUNT];
        Pipe pipe = Pipe.open();
        Pipe.SinkChannel sink = pipe.sink();
        Pipe.SourceChannel source = pipe.source();
        Selector sel = Selector.open();
        source.configureBlocking(false);
        source.register(sel, SelectionKey.OP_READ);

        for (int i = 0; i< PIPES_COUNT; i++ ) {
            pipes[i]= Pipe.open();
            Pipe.SourceChannel sc = pipes[i].source();
            sc.configureBlocking(false);
            sc.register(sel, SelectionKey.OP_READ);
            Pipe.SinkChannel sc2 = pipes[i].sink();
            sc2.configureBlocking(false);
            sc2.register(sel, SelectionKey.OP_WRITE);
        }

        for (int i = 0 ; i < LOOPS; i++ ) {
            sink.write(ByteBuffer.allocate(BUF_SIZE));
            int x = sel.selectNow();
            sel.selectedKeys().clear();
            source.read(ByteBuffer.allocate(BUF_SIZE));
        }

        for (int i = 0; i < PIPES_COUNT; i++ ) {
            pipes[i].sink().close();
            pipes[i].source().close();
        }
        pipe.sink().close();
        pipe.source().close();
        sel.close();
    }
}
