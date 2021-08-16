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

package jdk.jfr.internal;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jdk.jfr.SettingControl;
import jdk.jfr.internal.settings.JDKSettingControl;

public final class Control {
    @SuppressWarnings("removal")
    private final AccessControlContext context;
    private static final int CACHE_SIZE = 5;
    private final Set<?>[] cachedUnions = new HashSet<?>[CACHE_SIZE];
    private final String[] cachedValues = new String[CACHE_SIZE];
    private final SettingControl delegate;
    private String defaultValue;
    private String lastValue;

    // called by exposed subclass in external API
    public Control(SettingControl delegate, String defaultValue) {
        this.context = PrivateAccess.getInstance().getContext(delegate);
        this.delegate = delegate;
        this.defaultValue = defaultValue;
        if (this.context == null && !(delegate instanceof JDKSettingControl)) {
            throw new InternalError("Security context can only be null for trusted setting controls");
        }
    }

    boolean isType(Class<? extends SettingControl> clazz) {
        return delegate.getClass() == clazz;
    }

    final void apply(Set<String> values) {
        setValue(findCombine(values));
    }

    final void setDefault() {
        if (defaultValue == null) {
            defaultValue = getValue();
        }
        apply(defaultValue);
    }

    @SuppressWarnings("removal")
    public String getValue() {
        if (context == null) {
            // VM events requires no access control context
            return getValue();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    try {
                        return delegate.getValue();
                    } catch (Throwable t) {
                        // Prevent malicious user to propagate exception callback in the wrong context
                        Logger.log(LogTag.JFR_SETTING, LogLevel.WARN, "Exception occurred when trying to get value for " + getClass());
                    }
                    return defaultValue != null ? defaultValue : ""; // Need to return something
                }
            }, context);
        }
    }

    private void apply(String value) {
        if (lastValue != null && Objects.equals(value, lastValue)) {
            return;
        }
        setValue(value);
    }

    @SuppressWarnings("removal")
    public void setValue(String value) {
        if (context == null) {
            // VM events requires no access control context
            try {
                delegate.setValue(value);
            } catch (Throwable t) {
                Logger.log(LogTag.JFR_SETTING, LogLevel.WARN, "Exception occurred when setting value \"" + value + "\" for " + getClass());
            }
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    try {
                        delegate.setValue(value);
                    } catch (Throwable t) {
                        // Prevent malicious user to propagate exception callback in the wrong context
                        Logger.log(LogTag.JFR_SETTING, LogLevel.WARN, "Exception occurred when setting value \"" + value + "\" for " + getClass());
                    }
                    return null;
                }
            }, context);
        }
        lastValue = value;
    }


    @SuppressWarnings("removal")
    public String combine(Set<String> values) {
        if (context == null) {
            // VM events requires no access control context
            return delegate.combine(values);
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    delegate.combine(Collections.unmodifiableSet(values));
                } catch (Throwable t) {
                    // Prevent malicious user to propagate exception callback in the wrong context
                    Logger.log(LogTag.JFR_SETTING, LogLevel.WARN, "Exception occurred when combining " + values + " for " + getClass());
                }
                return null;
            }
        }, context);
    }

    private final String findCombine(Set<String> values) {
        if (values.size() == 1) {
            return values.iterator().next();
        }
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (Objects.equals(cachedUnions[i], values)) {
                return cachedValues[i];
            }
        }
        String result = combine(values);
        for (int i = 0; i < CACHE_SIZE - 1; i++) {
            cachedUnions[i + 1] = cachedUnions[i];
            cachedValues[i + 1] = cachedValues[i];
        }
        cachedValues[0] = result;
        cachedUnions[0] = values;
        return result;
    }

    final String getDefaultValue() {
        return defaultValue;
    }

    final String getLastValue() {
        return lastValue;
    }
}
