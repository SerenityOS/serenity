/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.resources;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * BreakIteratorResourceBundle is an abstract class for loading BreakIterator
 * data (rules or dictionary) from each module. An implementation class must
 * implement getBreakIteratorInfo() that returns an instance of the
 * corresponding BreakIteratorInfo (basename).  The data name is taken from the
 * BreakIteratorInfo instance.
 *
 * <p>For example, if the given key is "WordDictionary" and Locale is "th", the
 * data name is taken from a BreakIteratorInfo_th and the key's value is
 * "thai_dict".  Its data thai_dict is loaded from the Module of the
 * implementation class of this class.
 */

public abstract class BreakIteratorResourceBundle extends ResourceBundle {
    // If any keys that are not for data names are added to BreakIteratorInfo*,
    // those keys must be added to NON_DATA_KEYS.
    private static final Set<String> NON_DATA_KEYS = Set.of("BreakIteratorClasses");

    private volatile Set<String> keys;

    /**
     * Returns an instance of the corresponding {@code BreakIteratorInfo} (basename).
     * The instance shouldn't have its parent.
     */
    protected abstract ResourceBundle getBreakIteratorInfo();

    @Override
    protected Object handleGetObject(String key) {
        if (NON_DATA_KEYS.contains(key)) {
            return null;
        }
        ResourceBundle info = getBreakIteratorInfo();
        if (!info.containsKey(key)) {
            return null;
        }
        String path = getClass().getPackageName().replace('.', '/')
                      + '/' + info.getString(key);
        byte[] data;
        try (InputStream is = getResourceAsStream(path)) {
            data = is.readAllBytes();
        } catch (Exception e) {
            throw new InternalError("Can't load " + path, e);
        }
        return data;
    }

    @SuppressWarnings("removal")
    private InputStream getResourceAsStream(String path) throws Exception {
        PrivilegedExceptionAction<InputStream> pa;
        pa = () -> getClass().getModule().getResourceAsStream(path);
        InputStream is;
        try {
            is = AccessController.doPrivileged(pa);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
        return is;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(keySet());
    }

    @Override
    protected Set<String> handleKeySet() {
        if (keys == null) {
            ResourceBundle info = getBreakIteratorInfo();
            Set<String> k = info.keySet();
            k.removeAll(NON_DATA_KEYS);
            synchronized (this) {
                if (keys == null) {
                    keys = k;
                }
            }
        }
        return keys;
    }
}
