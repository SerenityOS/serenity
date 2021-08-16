/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.process;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class StreamReader extends Thread {
        private InputStream in;
        private List<StreamListener> listeners;
        private volatile boolean terminate = false;
        private static final long CLEANUP_TIMEOUT = 60000;

        public StreamReader(String desc) {
                super("Stream Reader: " + desc);
                setDaemon(true);
        }

        public StreamReader(String desc, InputStream in) {
                this(desc);
                setStream(in);
        }

        public void setDescription(String desc) {
                setName("Stream Reader: " + desc);
        }

        public void setStream(InputStream in) {
                this.in = in;
        }

        public synchronized void addListener(StreamListener listener) {
                if (listeners == null)
                        listeners = new ArrayList<StreamListener>();
                listeners.add(listener);
        }

        public synchronized void removeListener(StreamListener listener) {
                if (listeners != null) {
                        while (listeners.remove(listener))
                                ;
                }
        }

        public void run() {
                onStart();
                BufferedReader rd;
                try {
                        rd = new BufferedReader(new InputStreamReader(in));
                        String line;
                        while (!terminate) {
                                line = rd.readLine();
                                if (line == null)
                                        break;
                                onRead(line);
                                while (rd.ready()) {
                                        line = rd.readLine();
                                        if (line == null)
                                                break;
                                        onRead(line);
                                }
                                if (line == null)
                                        break;
                        }
                        cleanup();
                        onFinish();
                } catch (IOException e) {
                        if (!terminate)
                                onException(e);
                        else
                                onFinish();
                }
        }

        protected void onStart() {
                if (listeners != null) {
                        for (StreamListener l : listeners)
                                l.onStart();
                }
        }

        protected void onRead(String line) {
                //System.out.println("Read: " + line);
                if (listeners != null) {
                        for (StreamListener l : listeners)
                                l.onRead(line);
                }
        }

        protected void onFinish() {
                if (listeners != null) {
                        for (StreamListener l : listeners)
                                l.onFinish();
                }
        }

        protected void onException(Throwable e) {
                if (listeners != null) {
                        for (StreamListener l : listeners)
                                l.onException(e);
                }
        }

        private void cleanup() {
                try {
                        in.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        public void kill() {
                terminate = true;
                try {
                        this.join(CLEANUP_TIMEOUT);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
                this.interrupt();
                try {
                        this.join(CLEANUP_TIMEOUT);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
        }
}
