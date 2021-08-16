/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.io;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.jfr.EventField;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

// Contains data from a JFR IO event.
public class IOEvent {
    public final String thread;
    public final EventType eventType;
    public final long size;
    public final String address;  // Name of file or socket address.
    public final boolean endOfStream;

    // Constructor is private. Use IOEvent.create...
    private IOEvent(String thread, EventType eventType, long size, String address, boolean endOfStream) {
        this.thread = thread;
        this.eventType = eventType;
        this.size = size;
        this.address = address;
        this.endOfStream = endOfStream;
    }

    @Override
    public String toString() {
        String key = String.format("thread: %s, type: %s, size: %d, address: %s endOfStream: %s", thread, eventType, size, address, endOfStream);
        return key;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof IOEvent)) {
            return false;
        }
        return toString().equals(object.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public static final String EVENT_UNKNOWN = "unknown-event??";
    public static final String EVENT_FILE_FORCE = EventNames.FileForce;
    public static final String EVENT_FILE_READ = EventNames.FileRead;
    public static final String EVENT_FILE_WRITE = EventNames.FileWrite;
    public static final String EVENT_SOCKET_READ = EventNames.SocketRead;
    public static final String EVENT_SOCKET_WRITE = EventNames.SocketWrite;

    public enum EventType { UnknownEvent, FileForce, FileRead, FileWrite, SocketRead, SocketWrite }

    private static final String[] eventPaths = {
        EVENT_UNKNOWN, EVENT_FILE_FORCE, EVENT_FILE_READ, EVENT_FILE_WRITE, EVENT_SOCKET_READ, EVENT_SOCKET_WRITE
    };

    public static boolean isWriteEvent(EventType eventType) {
        return (eventType == EventType.SocketWrite || eventType == EventType.FileWrite);
    }

    public static boolean isReadEvent(EventType eventType) {
        return (eventType == EventType.SocketRead || eventType == EventType.FileRead);
    }

    public static boolean isFileEvent(EventType eventType) {
        return (eventType == EventType.FileForce || eventType == EventType.FileWrite || eventType == EventType.FileRead);
    }

    public static IOEvent createSocketWriteEvent(long size, Socket s) {
        if (size < 0) {
            size = 0;
        }
        return new IOEvent(Thread.currentThread().getName(), EventType.SocketWrite, size, getAddress(s), false);
    }

    public static IOEvent createSocketReadEvent(long size, Socket s) {
        boolean endOfStream = false;
        if (size < 0) {
            size = 0;
            endOfStream = true;
        }
        return new IOEvent(Thread.currentThread().getName(), EventType.SocketRead, size, getAddress(s), endOfStream);
    }

    public static IOEvent createFileForceEvent(File file) {
        String address = null;
        try {
            address = file.getCanonicalPath();
        } catch(IOException ex) {
            throw new RuntimeException();
        }
        return new IOEvent(Thread.currentThread().getName(), EventType.FileForce, 0, address, false);
    }

    public static IOEvent createFileReadEvent(long size, File file) {
        boolean endOfStream = false;
        if (size < 0) {
            endOfStream = true;
            size = 0;
        }
        String address = null;
        try {
            address = file.getCanonicalPath();
        } catch(IOException ex) {
                throw new RuntimeException();
        }
        return new IOEvent(Thread.currentThread().getName(), EventType.FileRead, size, address, endOfStream);
    }

    public static IOEvent createFileWriteEvent(long size, File file) {
        if (size < 0) {
            size = 0;
        }
        String address = null;
        try {
            address = file.getCanonicalPath();
        } catch(IOException ex) {
                throw new RuntimeException();
        }
        return new IOEvent(Thread.currentThread().getName(), EventType.FileWrite, size, address, false);
    }

    public static EventType getEventType(RecordedEvent event) {
        final String path = event.getEventType().getName();
        for (int i = 0; i < eventPaths.length; ++i) {
            if (path.endsWith(eventPaths[i])) {
                return EventType.values()[i];
            }
        }
        return EventType.UnknownEvent;
    }

    public static IOEvent createTestEvent(RecordedEvent event) {
        EventType eventType = getEventType(event);
        if (eventType == EventType.UnknownEvent) {
            return null;
        }
        EventField ev = Events.assertField(event, "eventThread");
        RecordedThread t = ev.getValue();
        String thread = t.getJavaName();
        long size = 0L;
        if (isWriteEvent(eventType)) {
            size = Events.assertField(event, "bytesWritten").getValue();
        } else if (isReadEvent(eventType)) {
            size = Events.assertField(event, "bytesRead").getValue();
        }
        String address = getEventAddress(event);
        boolean endOfStream = false;
        if (event.hasField("endOfStream")) {
            endOfStream = event.getValue("endOfStream");
        }
        if (event.hasField("endOfFile")) {
            endOfStream = event.getValue("endOfFile");
        }
        return new IOEvent(thread, eventType, size, address, endOfStream);
    }

    public static String getEventAddress(RecordedEvent event) {
        if (isFileEvent(getEventType(event))) {
            String address = Events.assertField(event, "path").getValue();
            // must ensure canonical format
            String canonical_path = null;
            try {
                canonical_path = new File(address).getCanonicalPath();
            } catch (IOException ex) {
                throw new RuntimeException();
            }
            return canonical_path;
        } else {
            return event.getValue("address") + ":"  + event.getValue("port");
        }
    }

    private static String getAddress(Socket s) {
        return s.getInetAddress().getHostAddress() + ":" + s.getPort();
    }
}
