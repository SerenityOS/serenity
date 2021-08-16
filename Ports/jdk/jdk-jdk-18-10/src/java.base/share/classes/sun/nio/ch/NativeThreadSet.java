/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.nio.ch;


// Special-purpose data structure for sets of native threads


class NativeThreadSet {

    private long[] elts;
    private int used = 0;
    private boolean waitingToEmpty;

    NativeThreadSet(int n) {
        elts = new long[n];
    }

    // Adds the current native thread to this set, returning its index so that
    // it can efficiently be removed later.
    //
    int add() {
        long th = NativeThread.current();
        // 0 and -1 are treated as placeholders, not real thread handles
        if (th == 0)
            th = -1;
        synchronized (this) {
            int start = 0;
            if (used >= elts.length) {
                int on = elts.length;
                int nn = on * 2;
                long[] nelts = new long[nn];
                System.arraycopy(elts, 0, nelts, 0, on);
                elts = nelts;
                start = on;
            }
            for (int i = start; i < elts.length; i++) {
                if (elts[i] == 0) {
                    elts[i] = th;
                    used++;
                    return i;
                }
            }
            assert false;
            return -1;
        }
    }

    // Removes the thread at the given index.
    //
    void remove(int i) {
        synchronized (this) {
            elts[i] = 0;
            used--;
            if (used == 0 && waitingToEmpty)
                notifyAll();
        }
    }

    // Signals all threads in this set.
    //
    synchronized void signalAndWait() {
        boolean interrupted = false;
        while (used > 0) {
            int u = used;
            int n = elts.length;
            for (int i = 0; i < n; i++) {
                long th = elts[i];
                if (th == 0)
                    continue;
                if (th != -1)
                    NativeThread.signal(th);
                if (--u == 0)
                    break;
            }
            waitingToEmpty = true;
            try {
                wait(50);
            } catch (InterruptedException e) {
                interrupted = true;
            } finally {
                waitingToEmpty = false;
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }
}
