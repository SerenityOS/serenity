/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A file lock table that is over a system-wide map of all file locks.
 */
class FileLockTable {
    /**
     * A weak reference to a FileLock.
     * <p>
     * FileLockTable uses a list of file lock references to avoid keeping the
     * FileLock (and FileChannel) alive.
     */
    private static class FileLockReference extends WeakReference<FileLock> {
        private FileKey fileKey;

        FileLockReference(FileLock referent,
                          ReferenceQueue<FileLock> queue,
                          FileKey key) {
            super(referent, queue);
            this.fileKey = key;
        }

        FileKey fileKey() {
            return fileKey;
        }
    }

    // The system-wide map is a ConcurrentHashMap that is keyed on the FileKey.
    // The map value is a list of file locks represented by FileLockReferences.
    // All access to the list must be synchronized on the list.
    private static ConcurrentHashMap<FileKey, List<FileLockReference>> lockMap =
        new ConcurrentHashMap<FileKey, List<FileLockReference>>();

    // reference queue for cleared refs
    private static ReferenceQueue<FileLock> queue = new ReferenceQueue<FileLock>();

    // The connection to which this table is connected
    private final Channel channel;

    // File key for the file that this channel is connected to
    private final FileKey fileKey;

    // Locks obtained for this channel
    private final Set<FileLock> locks;

    /**
     * Creates a file lock table for a channel that is connected to the
     * system-wide map of all file locks for the Java virtual machine.
     */
    FileLockTable(Channel channel, FileDescriptor fd) throws IOException {
        this.channel = channel;
        this.fileKey = FileKey.create(fd);
        this.locks = new HashSet<FileLock>();
    }

    void add(FileLock fl) throws OverlappingFileLockException {
        List<FileLockReference> list = lockMap.get(fileKey);

        for (;;) {

            // The key isn't in the map so we try to create it atomically
            if (list == null) {
                list = new ArrayList<FileLockReference>(2);
                List<FileLockReference> prev;
                synchronized (list) {
                    prev = lockMap.putIfAbsent(fileKey, list);
                    if (prev == null) {
                        // we successfully created the key so we add the file lock
                        list.add(new FileLockReference(fl, queue, fileKey));
                        locks.add(fl);
                        break;
                    }
                }
                // someone else got there first
                list = prev;
            }

            // There is already a key. It is possible that some other thread
            // is removing it so we re-fetch the value from the map. If it
            // hasn't changed then we check the list for overlapping locks
            // and add the new lock to the list.
            synchronized (list) {
                List<FileLockReference> current = lockMap.get(fileKey);
                if (list == current) {
                    checkList(list, fl.position(), fl.size());
                    list.add(new FileLockReference(fl, queue, fileKey));
                    locks.add(fl);
                    break;
                }
                list = current;
            }

        }

        // process any stale entries pending in the reference queue
        removeStaleEntries();
    }

    private void removeKeyIfEmpty(FileKey fk, List<FileLockReference> list) {
        assert Thread.holdsLock(list);
        assert lockMap.get(fk) == list;
        if (list.isEmpty()) {
            lockMap.remove(fk);
        }
    }

    void remove(FileLock fl) {
        assert fl != null;

        // the lock must exist so the list of locks must be present
        List<FileLockReference> list = lockMap.get(fileKey);
        if (list == null) return;

        synchronized (list) {
            int index = 0;
            while (index < list.size()) {
                FileLockReference ref = list.get(index);
                FileLock lock = ref.get();
                if (lock == fl) {
                    assert (lock != null) && (lock.acquiredBy() == channel);
                    ref.clear();
                    list.remove(index);
                    locks.remove(fl);
                    break;
                }
                index++;
            }
        }
    }

    List<FileLock> removeAll() {
        List<FileLock> result = new ArrayList<FileLock>();
        List<FileLockReference> list = lockMap.get(fileKey);
        if (list != null) {
            synchronized (list) {
                int index = 0;
                while (index < list.size()) {
                    FileLockReference ref = list.get(index);
                    FileLock lock = ref.get();

                    // remove locks obtained by this channel
                    if (lock != null && lock.acquiredBy() == channel) {
                        // remove the lock from the list
                        ref.clear();
                        list.remove(index);

                        // add to result
                        result.add(lock);
                    } else {
                        index++;
                    }
                }

                // once the lock list is empty we remove it from the map
                removeKeyIfEmpty(fileKey, list);

                locks.clear();
            }
        }
        return result;
    }

    void replace(FileLock fromLock, FileLock toLock) {
        // the lock must exist so there must be a list
        List<FileLockReference> list = lockMap.get(fileKey);
        assert list != null;

        synchronized (list) {
            for (int index=0; index<list.size(); index++) {
                FileLockReference ref = list.get(index);
                FileLock lock = ref.get();
                if (lock == fromLock) {
                    ref.clear();
                    list.set(index, new FileLockReference(toLock, queue, fileKey));
                    locks.remove(fromLock);
                    locks.add(toLock);
                    break;
                }
            }
        }
    }

    // Check for overlapping file locks
    private void checkList(List<FileLockReference> list, long position, long size)
        throws OverlappingFileLockException
    {
        assert Thread.holdsLock(list);
        for (FileLockReference ref: list) {
            FileLock fl = ref.get();
            if (fl != null && fl.overlaps(position, size))
                throw new OverlappingFileLockException();
        }
    }

    // Process the reference queue
    private void removeStaleEntries() {
        FileLockReference ref;
        while ((ref = (FileLockReference)queue.poll()) != null) {
            FileKey fk = ref.fileKey();
            List<FileLockReference> list = lockMap.get(fk);
            if (list != null) {
                synchronized (list) {
                    list.remove(ref);
                    removeKeyIfEmpty(fk, list);
                }
            }
        }
    }
}
