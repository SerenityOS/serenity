/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.cmm.lcms;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

import sun.java2d.cmm.Profile;

final class LCMSProfile extends Profile {

    private final Object disposerReferent;
    private final Map<Integer, byte[]> tags = new ConcurrentHashMap<>();
    private final StampedLock lock = new StampedLock();

    LCMSProfile(long ptr, Object ref) {
        super(ptr);
        disposerReferent = ref;
    }

    long getLcmsPtr() {
        return getNativePtr();
    }

    byte[] getProfileData() {
        long stamp = lock.readLock();
        try {
            return LCMS.getProfileDataNative(getNativePtr());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    byte[] getTag(int sig) {
        byte[] t = tags.get(sig);
        if (t != null) {
            return t;
        }
        long stamp = lock.readLock();
        try {
            return tags.computeIfAbsent(sig, (key) -> {
                return LCMS.getTagNative(getNativePtr(), key);
            });
        } finally {
            lock.unlockRead(stamp);
        }
    }

    void setTag(int tagSignature, byte[] data) {
        long stamp = lock.writeLock();
        try {
            tags.clear();
            // Now we are going to update the profile with new tag data
            // In some cases, we may change the pointer to the native profile.
            //
            // If we fail to write tag data for any reason, the old pointer
            // should be used.
            LCMS.setTagDataNative(getNativePtr(), tagSignature, data);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
