/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc.lock.jni;

import nsk.share.TestBug;
import nsk.share.gc.lock.Lockers;
import nsk.share.gc.lock.Locker;

public class JNILockers implements Lockers {
        public Locker createLocker(Object obj) {
                if (obj instanceof String)
                        return new StringCriticalLocker((String) obj);
                if (obj instanceof boolean[])
                        return new BooleanArrayCriticalLocker((boolean[]) obj);
                if (obj instanceof byte[])
                        return new ByteArrayCriticalLocker((byte[]) obj);
                if (obj instanceof char[])
                        return new CharArrayCriticalLocker((char[]) obj);
                if (obj instanceof double[])
                        return new DoubleArrayCriticalLocker((double[]) obj);
                if (obj instanceof float[])
                        return new FloatArrayCriticalLocker((float[]) obj);
                if (obj instanceof int[])
                        return new IntArrayCriticalLocker((int[]) obj);
                if (obj instanceof long[])
                        return new LongArrayCriticalLocker((long[]) obj);
                if (obj instanceof short[])
                        return new ShortArrayCriticalLocker((short[]) obj);
                throw new TestBug("Cannot create locker for: " + obj);
        }
}
