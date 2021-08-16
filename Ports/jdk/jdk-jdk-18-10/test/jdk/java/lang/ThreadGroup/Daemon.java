/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4950216 4089701 4956093
 * @summary Test for premature destruction of daemon threadgroups
 */

public class Daemon {

    public static void main(String args[]) throws Exception {
        ThreadGroup tg = new ThreadGroup("madbot-threads");
        Thread myThread = new MadThread(tg,"mad");
        ThreadGroup aGroup = new ThreadGroup(tg, "ness");
        tg.setDaemon(true);
        if (tg.activeCount() != 0)
            throw new RuntimeException("activeCount");
        aGroup.destroy();
        if (tg.isDestroyed())
            throw new RuntimeException("destroy");
        try {
            Thread anotherThread = new MadThread(aGroup, "bot");
            throw new RuntimeException("illegal");
        } catch (IllegalThreadStateException itse) {
            // Correct result
        }
    }
}

class MadThread extends Thread {
    String name;
    MadThread(ThreadGroup tg, String name) {
        super(tg, name);
        this.name = name;
    }
    public void run() {
        System.out.println("me run "+name);
    }
}
