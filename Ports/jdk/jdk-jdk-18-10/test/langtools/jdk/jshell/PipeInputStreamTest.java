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
 * @test 8167461
 * @summary Verify PipeInputStream works.
 * @modules jdk.compiler/com.sun.tools.javac.util
 *          jdk.jshell/jdk.jshell.execution:open
 * @run testng PipeInputStreamTest
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.annotations.Test;

import com.sun.tools.javac.util.Pair;

import static org.testng.Assert.*;

@Test
public class PipeInputStreamTest {

    public void testReadArrayNotBlocking() throws Exception {
        Pair<InputStream, OutputStream> streams = createPipeStream();
        InputStream in = streams.fst;
        OutputStream out = streams.snd;
        out.write('a');
        byte[] data = new byte[12];
        assertEquals(in.read(data), 1);
        assertEquals(data[0], 'a');
        out.write('a'); out.write('b'); out.write('c');
        assertEquals(in.read(data), 3);
        assertEquals(data[0], 'a');
        assertEquals(data[1], 'b');
        assertEquals(data[2], 'c');
    }

    private Pair<InputStream, OutputStream> createPipeStream() throws Exception {
        Class<?> pipeStreamClass = Class.forName("jdk.jshell.execution.PipeInputStream");
        Constructor<?> c = pipeStreamClass.getDeclaredConstructor();
        c.setAccessible(true);
        Object pipeStream = c.newInstance();
        Method createOutputStream = pipeStreamClass.getDeclaredMethod("createOutput");
        createOutputStream.setAccessible(true);
        return Pair.of((InputStream) pipeStream, (OutputStream) createOutputStream.invoke(pipeStream));
    }

}
