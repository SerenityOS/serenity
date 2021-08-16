/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4162583 7054918 8130181
 * @library ../testlibrary
 * @summary Make sure Provider api implementations are synchronized properly
 */

import java.security.*;

public class SynchronizedAccess {

    public static void main(String[] args) throws Exception {
        ProvidersSnapshot snapshot = ProvidersSnapshot.create();
        try {
            main0(args);
        } finally {
            snapshot.restore();
        }
    }

    public static void main0(String[] args) throws Exception {
        AccessorThread[] acc = new AccessorThread[200];
        for (int i=0; i < acc.length; i++)
            acc[i] = new AccessorThread("thread"+i);
        for (int i=0; i < acc.length; i++)
            acc[i].start();
        for (int i=0; i < acc.length; i++)
            acc[i].join();
    }
}

class AccessorThread extends Thread {

    public AccessorThread(String str) {
        super(str);
    }

    public void run() {
        Provider[] provs = new Provider[10];
        for (int i=0; i < provs.length; i++)
            provs[i] = new MyProvider("name"+i, "1", "test");

        int rounds = 20;
        while (rounds-- > 0) {
            try {
                for (int i=0; i<provs.length; i++) {
                    Security.addProvider(provs[i]);
                }
                Signature sig = Signature.getInstance("sigalg");
                for (int i=0; i<provs.length; i++) {
                    Security.removeProvider("name"+i);
                }
                provs = Security.getProviders();
            } catch (NoSuchAlgorithmException nsae) {
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
            }
        } // while
    }
}

class MyProvider extends Provider {
    public MyProvider(String name, String version, String info) {
        super(name, version, info);
        put("Signature.sigalg", "sigimpl");
    }
}
