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

import java.util.List;
import java.util.ArrayList;
import nsk.share.TestBug;

public class StreamMessageInput implements MessageInput {
        private Object sync = new Object();
        private List<String> lines = new ArrayList<String>();
        private int position = 0;
        private volatile boolean active = false;
        private volatile Throwable exception;

        public StreamMessageInput() {
        }

        public StreamMessageInput(StreamReader sd) {
                bind(sd);
        }

        public StreamListener createListener() {
                return new Listener();
        }

        public void bind(StreamReader sd) {
                sd.addListener(createListener());
        }

        public boolean isActive() {
                return active;
        }

        public boolean isException() {
                return exception != null;
        }

        public Throwable getException() {
                return exception;
        }

        public boolean waitForStart(long timeout) throws InterruptedException {
                long startTime = System.currentTimeMillis();
                long curTime = startTime;
                synchronized (sync) {
                        while (!active && curTime - startTime < timeout) {
                                sync.wait(curTime - startTime);
                                curTime = System.currentTimeMillis();
                        }
                }
                return active;
        }

        public boolean waitForFinish(long timeout) throws InterruptedException {
                long startTime = System.currentTimeMillis();
                long curTime = startTime;
                synchronized (sync) {
                        while (active && curTime - startTime < timeout) {
                                sync.wait(curTime - startTime);
                                curTime = System.currentTimeMillis();
                        }
                }
                return !active;
        }

        public boolean waitForMessage(String msg, long timeout) throws InterruptedException {
                long startTime = System.currentTimeMillis();
                long curTime = startTime;
                int n = position;
                synchronized (sync) {
                        while (curTime - startTime < timeout) {
                                while (n < lines.size()) {
                                        // System.out.println("Check: " + lines.get(n));
                                        if (msg == null || lines.get(n++).contains(msg)) {
                                                return true;
                                        }
                                }
                                sync.wait(timeout - (curTime - startTime));
                                curTime = System.currentTimeMillis();
                        }
                        return false;
                }
        }

        public boolean waitForMessage(long timeout) throws InterruptedException {
                return waitForMessage(null, timeout);
        }

        public String getMessage() {
                if (position < lines.size())
                        return lines.get(position++);
                else
                        return null;
        }

        public String getMessage(int index) {
                return lines.get(index);
        }

        public int getPosition() {
                return position;
        }

        public void setPosition(int position) {
                this.position = position;
        }

        public int getMessageCount() {
                return lines.size();
        }

        public List<String> getMessages() {
                return getMessages(position, lines.size());
        }

        public List<String> getMessages(int to) {
                return getMessages(position, to);
        }

        public List<String> getMessages(int from, int to) {
                synchronized (sync) {
                        if (to < 0)
                                to = lines.size() + to;
                        position = Math.max(position, to);
                        return new ArrayList<String>(lines.subList(from, to));
                }
        }

        public void reset() {
                synchronized (sync) {
                        position = lines.size();
                }
        }

        private class Listener implements StreamListener {
                @Override
                public void onStart() {
                        synchronized (sync) {
                                active = true;
                                sync.notifyAll();
                        }
                }

                @Override
                public void onRead(String line) {
                        //System.out.println("onRead: " + line);
                        synchronized (sync) {
                                lines.add(line);
                                sync.notifyAll();
                        }
                }

                @Override
                public void onFinish() {
                        synchronized (sync) {
                                active = false;
                                sync.notifyAll();
                        }
                }

                @Override
                public void onException(Throwable e) {
                        exception = e;
                }
        }
}
