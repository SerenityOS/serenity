/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.util;

import java.util.HashMap;
import java.util.Map;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author Thomas
 */
public class LookupHistory {

    private static Map<Class, LookupHistoryImpl> cache = new HashMap<>();

    private static class LookupHistoryImpl<T> implements LookupListener {

        private Class<T> klass;
        private Result<T> result;
        private T last;

        public LookupHistoryImpl(Class<T> klass) {
            this.klass = klass;
            result = Utilities.actionsGlobalContext().lookupResult(klass);
            result.addLookupListener(this);
            last = Utilities.actionsGlobalContext().lookup(klass);
        }

        public T getLast() {
            return last;
        }

        @Override
        public void resultChanged(LookupEvent ev) {
            T current = Utilities.actionsGlobalContext().lookup(klass);
            if (current != null) {
                last = current;
            }
        }
    }

    public static <T> void init(Class<T> klass) {
        if (!cache.containsKey(klass)) {
            cache.put(klass, new LookupHistoryImpl<>(klass));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getLast(Class<T> klass) {
        init(klass);
        assert cache.containsKey(klass);
        return (T) cache.get(klass).getLast();
    }
}
