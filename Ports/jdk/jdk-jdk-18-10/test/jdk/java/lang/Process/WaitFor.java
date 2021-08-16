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

/*
 * @test
 * @bug 8220684
 * @summary Process.waitFor(long, TimeUnit) can return false for a process
 *          that exited within the timeout
 * @run main/othervm WaitFor
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class WaitFor {
    public static void main(String[] args) throws Throwable {
        int failCnt = 0;
        for (int i = 0; i < 30; ++i) {
            Process proc = new MyProcess(new ProcessBuilder("true").start());
            boolean exited = proc.waitFor(100, TimeUnit.MILLISECONDS);
            if (!exited && !proc.isAlive()) failCnt++;
        }
        if (failCnt > 10) {
            throw new RuntimeException(failCnt + " processes were still alive"
                + " after timeout");
        }
    }
}

/**
 * This class uses the default implementation of java.lang.Process#waitFor(long,
 * TimeUnit), and delegates all other calls to the actual implementation of
 * Process.
 */
class MyProcess extends Process {
    Process impl;
    public MyProcess(Process impl) { this.impl = impl; }
    public OutputStream getOutputStream() { return impl.getOutputStream(); }
    public InputStream getInputStream() { return impl.getInputStream(); }
    public InputStream getErrorStream() { return impl.getErrorStream(); }
    public int waitFor() throws InterruptedException { return impl.waitFor(); }
    public int exitValue() { return impl.exitValue(); }
    public void destroy() { impl.destroy(); }
    public ProcessHandle toHandle() { return impl.toHandle(); }
}
