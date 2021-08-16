/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef RESOURCEEDITOR_H
#define RESOURCEEDITOR_H

#include <windows.h>
#include <vector>
#include <string>


class ResourceEditor {
public:
    class FileLock {
    public:
        explicit FileLock(const std::wstring& binaryPath);
        explicit FileLock(HANDLE h);
        ~FileLock();

        HANDLE get() const {
            return h;
        }

        void discard(bool v = true) {
            theDiscard = v;
        }

        FileLock& ownHandle(bool v) {
            theOwnHandle = v;
            return *this;
        }

    private:
        FileLock(const FileLock&);
        FileLock& operator=(const FileLock&);
    private:
        HANDLE h;
        bool theOwnHandle;
        bool theDiscard;
    };

public:
    ResourceEditor();

    /**
     * Set the language identifier of the resource to be updated.
     */
    ResourceEditor& language(unsigned v) {
        lang = v;
        return *this;
    }

    /**
     * Set the resource type to be updated.
     */
    ResourceEditor& type(unsigned v);

    /**
     * Set the resource type to be updated.
     */
    ResourceEditor& type(LPCWSTR v);

    /**
     * Set resource ID.
     */
    ResourceEditor& id(unsigned v);

    /**
     * Set resource ID.
     */
    ResourceEditor& id(LPCWSTR v);

    /**
     * Replaces resource configured in the given binary with the given data stream.
     */
    ResourceEditor& apply(const FileLock& dstBinary, std::istream& srcStream, std::streamsize size=0);

    /**
     * Replaces resource configured in the given binary with contents of
     * the given binary file.
     */
    ResourceEditor& apply(const FileLock& dstBinary, const std::wstring& srcFile);

private:
    unsigned lang;
    std::wstring theId;
    LPCWSTR theIdPtr;
    std::wstring theType;
    LPCWSTR theTypePtr;
};

#endif // #ifndef RESOURCEEDITOR_H
