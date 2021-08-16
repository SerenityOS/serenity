/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jdk.jfr.SettingDescriptor;

/**
 * Implementation of event type.
 *
 * To avoid memory leaks, this class must not hold strong reference to an event
 * class or a setting class
 */
public final class PlatformEventType extends Type {
    private final boolean isJVM;
    private final boolean isJDK;
    private final boolean isMethodSampling;
    private final List<SettingDescriptor> settings = new ArrayList<>(5);
    private final boolean dynamicSettings;
    private final int stackTraceOffset;

    // default values
    private boolean largeSize = false;
    private boolean enabled = false;
    private boolean stackTraceEnabled = true;
    private long thresholdTicks = 0;
    private long period = 0;
    private boolean hasHook;

    private boolean beginChunk;
    private boolean endChunk;
    private boolean hasStackTrace = true;
    private boolean hasDuration = true;
    private boolean hasPeriod = true;
    private boolean hasCutoff = false;
    private boolean hasThrottle = false;
    private boolean isInstrumented;
    private boolean markForInstrumentation;
    private boolean registered = true;
    private boolean committable = enabled && registered;


    // package private
    PlatformEventType(String name, long id, boolean isJDK, boolean dynamicSettings) {
        super(name, Type.SUPER_TYPE_EVENT, id);
        this.dynamicSettings = dynamicSettings;
        this.isJVM = Type.isDefinedByJVM(id);
        this.isMethodSampling = isJVM && (name.equals(Type.EVENT_NAME_PREFIX + "ExecutionSample") || name.equals(Type.EVENT_NAME_PREFIX + "NativeMethodSample"));
        this.isJDK = isJDK;
        this.stackTraceOffset = stackTraceOffset(name, isJDK);
    }

    private static boolean isExceptionEvent(String name) {
        switch (name) {
            case Type.EVENT_NAME_PREFIX + "JavaErrorThrow" :
            case Type.EVENT_NAME_PREFIX + "JavaExceptionThrow" :
                return true;
        }
        return false;
    }

    private static boolean isUsingHandler(String name) {
        switch (name) {
            case Type.EVENT_NAME_PREFIX + "SocketRead"  :
            case Type.EVENT_NAME_PREFIX + "SocketWrite" :
            case Type.EVENT_NAME_PREFIX + "FileRead"    :
            case Type.EVENT_NAME_PREFIX + "FileWrite"   :
            case Type.EVENT_NAME_PREFIX + "FileForce"   :
                return true;
        }
        return false;
    }

    private static int stackTraceOffset(String name, boolean isJDK) {
        if (isJDK) {
            if (isExceptionEvent(name)) {
                return 4;
            }
            if (isUsingHandler(name)) {
                return 3;
            }
        }
        return 4;
    }

    public void add(SettingDescriptor settingDescriptor) {
        Objects.requireNonNull(settingDescriptor);
        settings.add(settingDescriptor);
    }

    public List<SettingDescriptor> getSettings() {
        if (dynamicSettings) {
            List<SettingDescriptor> list = new ArrayList<>(settings.size());
            for (SettingDescriptor s : settings) {
                if (Utils.isSettingVisible(s.getTypeId(), hasHook)) {
                    list.add(s);
                }
            }
            return list;
        }
        return settings;
    }

    public List<SettingDescriptor> getAllSettings() {
        return settings;
    }

    public void setHasStackTrace(boolean hasStackTrace) {
        this.hasStackTrace = hasStackTrace;
    }

    public void setHasDuration(boolean hasDuration) {
        this.hasDuration = hasDuration;
    }

    public void setHasCutoff(boolean hasCutoff) {
       this.hasCutoff = hasCutoff;
    }

    public void setHasThrottle(boolean hasThrottle) {
        this.hasThrottle = hasThrottle;
    }

    public void setCutoff(long cutoffNanos) {
        if (isJVM) {
            long cutoffTicks = Utils.nanosToTicks(cutoffNanos);
            JVM.getJVM().setCutoff(getId(), cutoffTicks);
        }
    }

    public void setThrottle(long eventSampleSize, long period_ms) {
        if (isJVM) {
            JVM.getJVM().setThrottle(getId(), eventSampleSize, period_ms);
        }
    }

    public void setHasPeriod(boolean hasPeriod) {
        this.hasPeriod = hasPeriod;
    }

    public boolean hasStackTrace() {
        return this.hasStackTrace;
    }

    public boolean hasDuration() {
        return this.hasDuration;
    }

    public boolean hasPeriod() {
        return this.hasPeriod;
    }

    public boolean hasCutoff() {
        return this.hasCutoff;
    }

    public boolean hasThrottle() {
        return this.hasThrottle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSystem() {
        return isJVM || isJDK;
    }

    public boolean isJVM() {
        return isJVM;
    }

    public boolean isJDK() {
        return isJDK;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateCommittable();
        if (isJVM) {
            if (isMethodSampling) {
                long p = enabled ? period : 0;
                JVM.getJVM().setMethodSamplingInterval(getId(), p);
            } else {
                JVM.getJVM().setEnabled(getId(), enabled);
            }
        }
    }

    public void setPeriod(long periodMillis, boolean beginChunk, boolean endChunk) {
        if (isMethodSampling) {
            long p = enabled ? periodMillis : 0;
            JVM.getJVM().setMethodSamplingInterval(getId(), p);
        }
        this.beginChunk = beginChunk;
        this.endChunk = endChunk;
        this.period = periodMillis;
    }

    public void setStackTraceEnabled(boolean stackTraceEnabled) {
        this.stackTraceEnabled = stackTraceEnabled;
        if (isJVM) {
            JVM.getJVM().setStackTraceEnabled(getId(), stackTraceEnabled);
        }
    }

    public void setThreshold(long thresholdNanos) {
        this.thresholdTicks = Utils.nanosToTicks(thresholdNanos);
        if (isJVM) {
            JVM.getJVM().setThreshold(getId(), thresholdTicks);
        }
    }

    public boolean isEveryChunk() {
        return period == 0;
    }

    public boolean getStackTraceEnabled() {
        return stackTraceEnabled;
    }

    public long getThresholdTicks() {
        return thresholdTicks;
    }

    public long getPeriod() {
        return period;
    }

    public boolean hasEventHook() {
        return hasHook;
    }

    public void setEventHook(boolean hasHook) {
        this.hasHook = hasHook;
    }

    public boolean isBeginChunk() {
        return beginChunk;
    }

    public boolean isEndChunk() {
        return endChunk;
    }

    public boolean isInstrumented() {
        return isInstrumented;
    }

    public void setInstrumented() {
        isInstrumented = true;
    }

    public void markForInstrumentation(boolean markForInstrumentation) {
        this.markForInstrumentation = markForInstrumentation;
    }

    public boolean isMarkedForInstrumentation() {
        return markForInstrumentation;
    }

    public boolean setRegistered(boolean registered) {
        if (this.registered != registered) {
            this.registered = registered;
            updateCommittable();
            LogTag logTag = isSystem() ? LogTag.JFR_SYSTEM_METADATA : LogTag.JFR_METADATA;
            if (registered) {
                Logger.log(logTag, LogLevel.INFO, "Registered " + getLogName());
            } else {
                Logger.log(logTag, LogLevel.INFO, "Unregistered " + getLogName());
            }
            if (!registered) {
                MetadataRepository.getInstance().setUnregistered();
            }
            return true;
        }
        return false;
    }

    private void updateCommittable() {
        this.committable = enabled && registered;
    }

    public final boolean isRegistered() {
        return registered;
    }

    // Efficient check of enabled && registered
    public boolean isCommittable() {
        return committable;
    }

    public int getStackTraceOffset() {
        return stackTraceOffset;
    }

    public boolean isLargeSize() {
        return largeSize;
    }

    public void setLargeSize() {
        largeSize = true;
    }
}
