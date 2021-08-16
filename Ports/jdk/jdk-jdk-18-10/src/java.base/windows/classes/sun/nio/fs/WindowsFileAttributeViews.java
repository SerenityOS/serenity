/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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


import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

class WindowsFileAttributeViews {

    private static class Basic extends AbstractBasicFileAttributeView {
        final WindowsPath file;
        final boolean followLinks;

        Basic(WindowsPath file, boolean followLinks) {
            this.file = file;
            this.followLinks = followLinks;
        }

        @Override
        public WindowsFileAttributes readAttributes() throws IOException {
            file.checkRead();
            try {
                return WindowsFileAttributes.get(file, followLinks);
            } catch (WindowsException x) {
                x.rethrowAsIOException(file);
                return null;    // keep compiler happy
            }
        }

        /**
         * Adjusts a Windows time for the FAT epoch.
         */
        private long adjustForFatEpoch(long time) {
            // 1/1/1980 in Windows Time
            final long FAT_EPOCH = 119600064000000000L;
            if (time != -1L && time < FAT_EPOCH) {
                return FAT_EPOCH;
            } else {
                return time;
            }
        }

        /**
         * Parameter values in Windows times.
         */
        void setFileTimes(long createTime,
                          long lastAccessTime,
                          long lastWriteTime)
            throws IOException
        {
            long handle = -1L;
            try {
                int flags = FILE_FLAG_BACKUP_SEMANTICS;
                if (!followLinks)
                    flags |= FILE_FLAG_OPEN_REPARSE_POINT;

                handle = CreateFile(file.getPathForWin32Calls(),
                                    FILE_WRITE_ATTRIBUTES,
                                    (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE),
                                    OPEN_EXISTING,
                                    flags);
            } catch (WindowsException x) {
                x.rethrowAsIOException(file);
            }

            // update times
            try {
                SetFileTime(handle,
                            createTime,
                            lastAccessTime,
                            lastWriteTime);
            } catch (WindowsException x) {
                // If ERROR_INVALID_PARAMETER is returned and the volume is
                // FAT then adjust to the FAT epoch and retry.
                if (followLinks && x.lastError() == ERROR_INVALID_PARAMETER) {
                    try {
                        if (WindowsFileStore.create(file).type().equals("FAT")) {
                            SetFileTime(handle,
                                        adjustForFatEpoch(createTime),
                                        adjustForFatEpoch(lastAccessTime),
                                        adjustForFatEpoch(lastWriteTime));
                            // retry succeeded
                            x = null;
                        }
                    } catch (SecurityException ignore) {
                    } catch (WindowsException ignore) {
                    } catch (IOException ignore) {
                        // ignore exceptions to let original exception be thrown
                    }
                }
                if (x != null)
                    x.rethrowAsIOException(file);
            } finally {
                CloseHandle(handle);
            }
        }

        @Override
        public void setTimes(FileTime lastModifiedTime,
                             FileTime lastAccessTime,
                             FileTime createTime) throws IOException
        {
            // if all null then do nothing
            if (lastModifiedTime == null && lastAccessTime == null &&
                createTime == null)
            {
                // no effect
                return;
            }

            // permission check
            file.checkWrite();

            // update times
            long t1 = (createTime == null) ? -1L :
                WindowsFileAttributes.toWindowsTime(createTime);
            long t2 = (lastAccessTime == null) ? -1L :
                WindowsFileAttributes.toWindowsTime(lastAccessTime);
            long t3 = (lastModifiedTime == null) ? -1L :
                WindowsFileAttributes.toWindowsTime(lastModifiedTime);
            setFileTimes(t1, t2, t3);
        }
    }

    static class Dos extends Basic implements DosFileAttributeView {
        private static final String READONLY_NAME = "readonly";
        private static final String ARCHIVE_NAME = "archive";
        private static final String SYSTEM_NAME = "system";
        private static final String HIDDEN_NAME = "hidden";
        private static final String ATTRIBUTES_NAME = "attributes";

        // the names of the DOS attributes (includes basic)
        static final Set<String> dosAttributeNames =
            Util.newSet(basicAttributeNames,
                        READONLY_NAME, ARCHIVE_NAME, SYSTEM_NAME,  HIDDEN_NAME, ATTRIBUTES_NAME);

        Dos(WindowsPath file, boolean followLinks) {
            super(file, followLinks);
        }

        @Override
        public String name() {
            return "dos";
        }

        @Override
        public void setAttribute(String attribute, Object value)
            throws IOException
        {
            if (attribute.equals(READONLY_NAME)) {
                setReadOnly((Boolean)value);
                return;
            }
            if (attribute.equals(ARCHIVE_NAME)) {
                setArchive((Boolean)value);
                return;
            }
            if (attribute.equals(SYSTEM_NAME)) {
                setSystem((Boolean)value);
                return;
            }
            if (attribute.equals(HIDDEN_NAME)) {
                setHidden((Boolean)value);
                return;
            }
            super.setAttribute(attribute, value);
        }

        @Override
        public Map<String,Object> readAttributes(String[] attributes)
            throws IOException
        {
            AttributesBuilder builder =
                AttributesBuilder.create(dosAttributeNames, attributes);
            WindowsFileAttributes attrs = readAttributes();
            addRequestedBasicAttributes(attrs, builder);
            if (builder.match(READONLY_NAME))
                builder.add(READONLY_NAME, attrs.isReadOnly());
            if (builder.match(ARCHIVE_NAME))
                builder.add(ARCHIVE_NAME, attrs.isArchive());
            if (builder.match(SYSTEM_NAME))
                builder.add(SYSTEM_NAME, attrs.isSystem());
            if (builder.match(HIDDEN_NAME))
                builder.add(HIDDEN_NAME, attrs.isHidden());
            if (builder.match(ATTRIBUTES_NAME))
                builder.add(ATTRIBUTES_NAME, attrs.attributes());
            return builder.unmodifiableMap();
        }

        /**
         * Update DOS attributes
         */
        private void updateAttributes(int flag, boolean enable)
            throws IOException
        {
            file.checkWrite();

            // GetFileAttributes & SetFileAttributes do not follow links so when
            // following links we need the final target
            String path = WindowsLinkSupport.getFinalPath(file, followLinks);
            try {
                int oldValue = GetFileAttributes(path);
                int newValue = oldValue;
                if (enable) {
                    newValue |= flag;
                } else {
                    newValue &= ~flag;
                }
                if (newValue != oldValue) {
                    SetFileAttributes(path, newValue);
                }
            } catch (WindowsException x) {
                // don't reveal target in exception
                x.rethrowAsIOException(file);
            }
        }

        @Override
        public void setReadOnly(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_READONLY, value);
        }

        @Override
        public void setHidden(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_HIDDEN, value);
        }

        @Override
        public void setArchive(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_ARCHIVE, value);
        }

        @Override
        public void setSystem(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_SYSTEM, value);
        }

        // package-private
        // Copy given attributes to the file.
        void setAttributes(WindowsFileAttributes attrs)
            throws IOException
        {
            // copy DOS attributes to target
            int flags = 0;
            if (attrs.isReadOnly()) flags |= FILE_ATTRIBUTE_READONLY;
            if (attrs.isHidden()) flags |= FILE_ATTRIBUTE_HIDDEN;
            if (attrs.isArchive()) flags |= FILE_ATTRIBUTE_ARCHIVE;
            if (attrs.isSystem()) flags |= FILE_ATTRIBUTE_SYSTEM;
            updateAttributes(flags, true);

            // copy file times to target - must be done after updating FAT attributes
            // as otherwise the last modified time may be wrong.
            setFileTimes(
                WindowsFileAttributes.toWindowsTime(attrs.creationTime()),
                WindowsFileAttributes.toWindowsTime(attrs.lastModifiedTime()),
                WindowsFileAttributes.toWindowsTime(attrs.lastAccessTime()));
        }
    }

    static Basic createBasicView(WindowsPath file, boolean followLinks) {
        return new Basic(file, followLinks);
    }

    static Dos createDosView(WindowsPath file, boolean followLinks) {
        return new Dos(file, followLinks);
    }
}
