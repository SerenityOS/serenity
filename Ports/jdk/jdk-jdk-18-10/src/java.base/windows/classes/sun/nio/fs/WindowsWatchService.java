/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/*
 * Win32 implementation of WatchService based on ReadDirectoryChangesW.
 */

class WindowsWatchService
    extends AbstractWatchService
{
    private static final int WAKEUP_COMPLETION_KEY = 0;

    // background thread to service I/O completion port
    private final Poller poller;

    /**
     * Creates an I/O completion port and a daemon thread to service it
     */
    WindowsWatchService(WindowsFileSystem fs) throws IOException {
        // create I/O completion port
        long port = 0L;
        try {
            port = CreateIoCompletionPort(INVALID_HANDLE_VALUE, 0, 0);
        } catch (WindowsException x) {
            throw new IOException(x.getMessage());
        }

        this.poller = new Poller(fs, this, port);
        this.poller.start();
    }

    @Override
    WatchKey register(Path path,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
         throws IOException
    {
        // delegate to poller
        return poller.register(path, events, modifiers);
    }

    @Override
    void implClose() throws IOException {
        // delegate to poller
        poller.close();
    }

    /**
     * Windows implementation of WatchKey.
     */
    private static class WindowsWatchKey extends AbstractWatchKey {
        // file key (used to detect existing registrations)
        private final FileKey fileKey;

        // handle to directory
        private volatile long handle = INVALID_HANDLE_VALUE;

        // interest events
        private Set<? extends WatchEvent.Kind<?>> events;

        // subtree
        private boolean watchSubtree;

        // buffer for change events
        private NativeBuffer buffer;

        // pointer to bytes returned (in buffer)
        private long countAddress;

        // pointer to overlapped structure (in buffer)
        private long overlappedAddress;

        // completion key (used to map I/O completion to WatchKey)
        private int completionKey;

        // flag indicates that ReadDirectoryChangesW failed
        // and overlapped I/O operation wasn't started
        private boolean errorStartingOverlapped;

        WindowsWatchKey(Path dir,
                        AbstractWatchService watcher,
                        FileKey fileKey)
        {
            super(dir, watcher);
            this.fileKey = fileKey;
        }

        WindowsWatchKey init(long handle,
                             Set<? extends WatchEvent.Kind<?>> events,
                             boolean watchSubtree,
                             NativeBuffer buffer,
                             long countAddress,
                             long overlappedAddress,
                             int completionKey)
        {
            this.handle = handle;
            this.events = events;
            this.watchSubtree = watchSubtree;
            this.buffer = buffer;
            this.countAddress = countAddress;
            this.overlappedAddress = overlappedAddress;
            this.completionKey = completionKey;
            return this;
        }

        long handle() {
            return handle;
        }

        Set<? extends WatchEvent.Kind<?>> events() {
            return events;
        }

        void setEvents(Set<? extends WatchEvent.Kind<?>> events) {
            this.events = events;
        }

        boolean watchSubtree() {
            return watchSubtree;
        }

        NativeBuffer buffer() {
            return buffer;
        }

        long countAddress() {
            return countAddress;
        }

        long overlappedAddress() {
            return overlappedAddress;
        }

        FileKey fileKey() {
            return fileKey;
        }

        int completionKey() {
            return completionKey;
        }

        void setErrorStartingOverlapped(boolean value) {
            errorStartingOverlapped = value;
        }

        boolean isErrorStartingOverlapped() {
            return errorStartingOverlapped;
        }

        // Invalidate the key, assumes that resources have been released
        void invalidate() {
            ((WindowsWatchService)watcher()).poller.releaseResources(this);
            handle = INVALID_HANDLE_VALUE;
            buffer = null;
            countAddress = 0;
            overlappedAddress = 0;
            errorStartingOverlapped = false;
        }

        @Override
        public boolean isValid() {
            return handle != INVALID_HANDLE_VALUE;
        }

        @Override
        public void cancel() {
            if (isValid()) {
                // delegate to poller
                ((WindowsWatchService)watcher()).poller.cancel(this);
            }
        }
    }

    // file key to unique identify (open) directory
    private static class FileKey {
        private final int volSerialNumber;
        private final int fileIndexHigh;
        private final int fileIndexLow;

        FileKey(int volSerialNumber, int fileIndexHigh, int fileIndexLow) {
            this.volSerialNumber = volSerialNumber;
            this.fileIndexHigh = fileIndexHigh;
            this.fileIndexLow = fileIndexLow;
        }

        @Override
        public int hashCode() {
            return volSerialNumber ^ fileIndexHigh ^ fileIndexLow;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof FileKey))
                return false;
            FileKey other = (FileKey)obj;
            if (this.volSerialNumber != other.volSerialNumber) return false;
            if (this.fileIndexHigh != other.fileIndexHigh) return false;
            return this.fileIndexLow == other.fileIndexLow;
        }
    }

    // all change events
    private static final int ALL_FILE_NOTIFY_EVENTS =
        FILE_NOTIFY_CHANGE_FILE_NAME |
        FILE_NOTIFY_CHANGE_DIR_NAME |
        FILE_NOTIFY_CHANGE_ATTRIBUTES  |
        FILE_NOTIFY_CHANGE_SIZE |
        FILE_NOTIFY_CHANGE_LAST_WRITE |
        FILE_NOTIFY_CHANGE_CREATION |
        FILE_NOTIFY_CHANGE_SECURITY;

    /**
     * Background thread to service I/O completion port.
     */
    private static class Poller extends AbstractPoller {
        private static final Unsafe UNSAFE = Unsafe.getUnsafe();

        /*
         * typedef struct _OVERLAPPED {
         *     ULONG_PTR  Internal;
         *     ULONG_PTR  InternalHigh;
         *     union {
         *         struct { DWORD Offset; DWORD OffsetHigh; };
         *         PVOID  Pointer;
         *     };
         *     HANDLE    hEvent;
         * } OVERLAPPED;
         */
        private static final short SIZEOF_DWORD         = 4;
        private static final short SIZEOF_OVERLAPPED    = 32; // 20 on 32-bit
        private static final short OFFSETOF_HEVENT      =
            (UNSAFE.addressSize() == 4) ? (short) 16 : 24;


        /*
         * typedef struct _FILE_NOTIFY_INFORMATION {
         *     DWORD NextEntryOffset;
         *     DWORD Action;
         *     DWORD FileNameLength;
         *     WCHAR FileName[1];
         * } FileNameLength;
         */
        private static final short OFFSETOF_NEXTENTRYOFFSET = 0;
        private static final short OFFSETOF_ACTION          = 4;
        private static final short OFFSETOF_FILENAMELENGTH  = 8;
        private static final short OFFSETOF_FILENAME        = 12;

        // size of per-directory buffer for events (FIXME - make this configurable)
        // Need to be less than 4*16384 = 65536. DWORD align.
        private static final int CHANGES_BUFFER_SIZE    = 16 * 1024;

        private final WindowsFileSystem fs;
        private final WindowsWatchService watcher;
        private final long port;

        // maps completion key to WatchKey
        private final Map<Integer, WindowsWatchKey> ck2key;

        // maps file key to WatchKey
        private final Map<FileKey, WindowsWatchKey> fk2key;

        // unique completion key for each directory
        // native completion key capacity is 64 bits on Win64.
        private int lastCompletionKey;

        Poller(WindowsFileSystem fs, WindowsWatchService watcher, long port) {
            this.fs = fs;
            this.watcher = watcher;
            this.port = port;
            this.ck2key = new HashMap<>();
            this.fk2key = new HashMap<>();
            this.lastCompletionKey = 0;
        }

        @Override
        void wakeup() throws IOException {
            try {
                PostQueuedCompletionStatus(port, WAKEUP_COMPLETION_KEY);
            } catch (WindowsException x) {
                throw new IOException(x.getMessage());
            }
        }

        /**
         * Register a directory for changes as follows:
         *
         * 1. Open directory
         * 2. Read its attributes (and check it really is a directory)
         * 3. Assign completion key and associated handle with completion port
         * 4. Call ReadDirectoryChangesW to start (async) read of changes
         * 5. Create or return existing key representing registration
         */
        @Override
        Object implRegister(Path obj,
                            Set<? extends WatchEvent.Kind<?>> events,
                            WatchEvent.Modifier... modifiers)
        {
            WindowsPath dir = (WindowsPath)obj;
            boolean watchSubtree = false;

            // FILE_TREE modifier allowed
            for (WatchEvent.Modifier modifier: modifiers) {
                if (ExtendedOptions.FILE_TREE.matches(modifier)) {
                    watchSubtree = true;
                } else {
                    if (modifier == null)
                        return new NullPointerException();
                    if (!ExtendedOptions.SENSITIVITY_HIGH.matches(modifier) &&
                            !ExtendedOptions.SENSITIVITY_MEDIUM.matches(modifier) &&
                            !ExtendedOptions.SENSITIVITY_LOW.matches(modifier)) {
                        return new UnsupportedOperationException("Modifier not supported");
                    }
                }
            }

            // open directory
            long handle;
            try {
                handle = CreateFile(dir.getPathForWin32Calls(),
                                    FILE_LIST_DIRECTORY,
                                    (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE),
                                    OPEN_EXISTING,
                                    FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED);
            } catch (WindowsException x) {
                return x.asIOException(dir);
            }

            boolean registered = false;
            try {
                // read attributes and check file is a directory
                WindowsFileAttributes attrs;
                try {
                    attrs = WindowsFileAttributes.readAttributes(handle);
                } catch (WindowsException x) {
                    return x.asIOException(dir);
                }
                if (!attrs.isDirectory()) {
                    return new NotDirectoryException(dir.getPathForExceptionMessage());
                }

                // check if this directory is already registered
                FileKey fk = new FileKey(attrs.volSerialNumber(),
                                         attrs.fileIndexHigh(),
                                         attrs.fileIndexLow());
                WindowsWatchKey existing = fk2key.get(fk);

                // if already registered and we're not changing the subtree
                // modifier then simply update the event and return the key.
                if (existing != null && watchSubtree == existing.watchSubtree()) {
                    existing.setEvents(events);
                    return existing;
                }

                // Can overflow the int type capacity.
                // Skip WAKEUP_COMPLETION_KEY value.
                int completionKey = ++lastCompletionKey;
                if (completionKey == WAKEUP_COMPLETION_KEY)
                    completionKey = ++lastCompletionKey;

                // associate handle with completion port
                try {
                    CreateIoCompletionPort(handle, port, completionKey);
                } catch (WindowsException x) {
                    return new IOException(x.getMessage());
                }

                // allocate memory for events, including space for other structures
                // needed to do overlapped I/O
                int size = CHANGES_BUFFER_SIZE + SIZEOF_DWORD + SIZEOF_OVERLAPPED;
                NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);

                long bufferAddress = buffer.address();
                long overlappedAddress = bufferAddress + size - SIZEOF_OVERLAPPED;
                long countAddress = overlappedAddress - SIZEOF_DWORD;

                // zero the overlapped structure
                UNSAFE.setMemory(overlappedAddress, SIZEOF_OVERLAPPED, (byte)0);

                // start async read of changes to directory
                try {
                    createAndAttachEvent(overlappedAddress);

                    ReadDirectoryChangesW(handle,
                                          bufferAddress,
                                          CHANGES_BUFFER_SIZE,
                                          watchSubtree,
                                          ALL_FILE_NOTIFY_EVENTS,
                                          countAddress,
                                          overlappedAddress);
                } catch (WindowsException x) {
                    closeAttachedEvent(overlappedAddress);
                    buffer.release();
                    return new IOException(x.getMessage());
                }

                WindowsWatchKey watchKey;
                if (existing == null) {
                    // not registered so create new watch key
                    watchKey = new WindowsWatchKey(dir, watcher, fk)
                        .init(handle, events, watchSubtree, buffer, countAddress,
                              overlappedAddress, completionKey);
                    // map file key to watch key
                    fk2key.put(fk, watchKey);
                } else {
                    // directory already registered so need to:
                    // 1. remove mapping from old completion key to existing watch key
                    // 2. release existing key's resources (handle/buffer)
                    // 3. re-initialize key with new handle/buffer
                    ck2key.remove(existing.completionKey());
                    releaseResources(existing);
                    watchKey = existing.init(handle, events, watchSubtree, buffer,
                        countAddress, overlappedAddress, completionKey);
                }
                // map completion map to watch key
                ck2key.put(completionKey, watchKey);

                registered = true;
                return watchKey;

            } finally {
                if (!registered) CloseHandle(handle);
            }
        }

        /**
         * Cancels the outstanding I/O operation on the directory
         * associated with the given key and releases the associated
         * resources.
         */
        private void releaseResources(WindowsWatchKey key) {
            if (!key.isErrorStartingOverlapped()) {
                try {
                    CancelIo(key.handle());
                    GetOverlappedResult(key.handle(), key.overlappedAddress());
                } catch (WindowsException expected) {
                    // expected as I/O operation has been cancelled
                }
            }
            CloseHandle(key.handle());
            closeAttachedEvent(key.overlappedAddress());
            key.buffer().free();
        }

        /**
         * Creates an unnamed event and set it as the hEvent field
         * in the given OVERLAPPED structure
         */
        private void createAndAttachEvent(long ov) throws WindowsException {
            long hEvent = CreateEvent(false, false);
            UNSAFE.putAddress(ov + OFFSETOF_HEVENT, hEvent);
        }

        /**
         * Closes the event attached to the given OVERLAPPED structure. A
         * no-op if there isn't an event attached.
         */
        private void closeAttachedEvent(long ov) {
            long hEvent = UNSAFE.getAddress(ov + OFFSETOF_HEVENT);
            if (hEvent != 0 && hEvent != INVALID_HANDLE_VALUE)
               CloseHandle(hEvent);
        }

        // cancel single key
        @Override
        void implCancelKey(WatchKey obj) {
            WindowsWatchKey key = (WindowsWatchKey)obj;
            if (key.isValid()) {
                fk2key.remove(key.fileKey());
                ck2key.remove(key.completionKey());
                key.invalidate();
            }
        }

        // close watch service
        @Override
        void implCloseAll() {
            // cancel all keys
            ck2key.values().forEach(WindowsWatchKey::invalidate);

            fk2key.clear();
            ck2key.clear();

            // close I/O completion port
            CloseHandle(port);
        }

        // Translate file change action into watch event
        private WatchEvent.Kind<?> translateActionToEvent(int action) {
            switch (action) {
                case FILE_ACTION_MODIFIED :
                    return StandardWatchEventKinds.ENTRY_MODIFY;

                case FILE_ACTION_ADDED :
                case FILE_ACTION_RENAMED_NEW_NAME :
                    return StandardWatchEventKinds.ENTRY_CREATE;

                case FILE_ACTION_REMOVED :
                case FILE_ACTION_RENAMED_OLD_NAME :
                    return StandardWatchEventKinds.ENTRY_DELETE;

                default :
                    return null;  // action not recognized
            }
        }

        // process events (list of FILE_NOTIFY_INFORMATION structures)
        private void processEvents(WindowsWatchKey key, int size) {
            long address = key.buffer().address();

            int nextOffset;
            do {
                int action = UNSAFE.getInt(address + OFFSETOF_ACTION);

                // map action to event
                WatchEvent.Kind<?> kind = translateActionToEvent(action);
                if (key.events().contains(kind)) {
                    // copy the name
                    int nameLengthInBytes = UNSAFE.getInt(address + OFFSETOF_FILENAMELENGTH);
                    if ((nameLengthInBytes % 2) != 0) {
                        throw new AssertionError("FileNameLength is not a multiple of 2");
                    }
                    char[] nameAsArray = new char[nameLengthInBytes/2];
                    UNSAFE.copyMemory(null, address + OFFSETOF_FILENAME, nameAsArray,
                        Unsafe.ARRAY_CHAR_BASE_OFFSET, nameLengthInBytes);

                    // create FileName and queue event
                    WindowsPath name = WindowsPath
                        .createFromNormalizedPath(fs, new String(nameAsArray));
                    key.signalEvent(kind, name);
                }

                // next event
                nextOffset = UNSAFE.getInt(address + OFFSETOF_NEXTENTRYOFFSET);
                address += (long)nextOffset;
            } while (nextOffset != 0);
        }

        /**
         * Poller main loop
         */
        @Override
        public void run() {
            for (;;) {
                CompletionStatus info;
                try {
                    info = GetQueuedCompletionStatus(port);
                } catch (WindowsException x) {
                    // this should not happen
                    x.printStackTrace();
                    return;
                }

                // wakeup
                if (info.completionKey() == WAKEUP_COMPLETION_KEY) {
                    boolean shutdown = processRequests();
                    if (shutdown) {
                        return;
                    }
                    continue;
                }

                // map completionKey to get WatchKey
                WindowsWatchKey key = ck2key.get((int)info.completionKey());
                if (key == null) {
                    // We get here when a registration is changed. In that case
                    // the directory is closed which causes an event with the
                    // old completion key.
                    continue;
                }

                boolean criticalError = false;
                int errorCode = info.error();
                int messageSize = info.bytesTransferred();
                if (errorCode == ERROR_NOTIFY_ENUM_DIR) {
                    // buffer overflow
                    key.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                } else if (errorCode != 0 && errorCode != ERROR_MORE_DATA) {
                    // ReadDirectoryChangesW failed
                    criticalError = true;
                } else {
                    // ERROR_MORE_DATA is a warning about incomplete
                    // data transfer over TCP/UDP stack. For the case
                    // [messageSize] is zero in the most of cases.

                    if (messageSize > 0) {
                        // process non-empty events.
                        processEvents(key, messageSize);
                    } else if (errorCode == 0) {
                        // insufficient buffer size
                        // not described, but can happen.
                        key.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                    }

                    // start read for next batch of changes
                    try {
                        ReadDirectoryChangesW(key.handle(),
                                              key.buffer().address(),
                                              CHANGES_BUFFER_SIZE,
                                              key.watchSubtree(),
                                              ALL_FILE_NOTIFY_EVENTS,
                                              key.countAddress(),
                                              key.overlappedAddress());
                    } catch (WindowsException x) {
                        // no choice but to cancel key
                        criticalError = true;
                        key.setErrorStartingOverlapped(true);
                    }
                }
                if (criticalError) {
                    implCancelKey(key);
                    key.signal();
                }
            }
        }
    }
}
