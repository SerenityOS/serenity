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

package jdk.jfr.internal.instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jdk.jfr.Event;
import jdk.jfr.events.ActiveRecordingEvent;
import jdk.jfr.events.ActiveSettingEvent;
import jdk.jfr.events.ContainerIOUsageEvent;
import jdk.jfr.events.ContainerConfigurationEvent;
import jdk.jfr.events.ContainerCPUUsageEvent;
import jdk.jfr.events.ContainerCPUThrottlingEvent;
import jdk.jfr.events.ContainerMemoryUsageEvent;
import jdk.jfr.events.DirectBufferStatisticsEvent;
import jdk.jfr.events.ErrorThrownEvent;
import jdk.jfr.events.ExceptionStatisticsEvent;
import jdk.jfr.events.ExceptionThrownEvent;
import jdk.jfr.events.FileForceEvent;
import jdk.jfr.events.FileReadEvent;
import jdk.jfr.events.FileWriteEvent;
import jdk.jfr.events.DeserializationEvent;
import jdk.jfr.events.ProcessStartEvent;
import jdk.jfr.events.SecurityPropertyModificationEvent;
import jdk.jfr.events.SocketReadEvent;
import jdk.jfr.events.SocketWriteEvent;
import jdk.jfr.events.TLSHandshakeEvent;
import jdk.jfr.events.X509CertificateEvent;
import jdk.jfr.events.X509ValidationEvent;
import jdk.jfr.internal.JVM;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.RequestEngine;
import jdk.jfr.internal.SecuritySupport;

import jdk.internal.platform.Container;
import jdk.internal.platform.Metrics;

public final class JDKEvents {
    private static final Class<?>[] mirrorEventClasses = {
        DeserializationEvent.class,
        ProcessStartEvent.class,
        SecurityPropertyModificationEvent.class,
        TLSHandshakeEvent.class,
        X509CertificateEvent.class,
        X509ValidationEvent.class
    };

    private static final Class<?>[] eventClasses = {
        FileForceEvent.class,
        FileReadEvent.class,
        FileWriteEvent.class,
        SocketReadEvent.class,
        SocketWriteEvent.class,
        ExceptionThrownEvent.class,
        ExceptionStatisticsEvent.class,
        ErrorThrownEvent.class,
        ActiveSettingEvent.class,
        ActiveRecordingEvent.class,
        jdk.internal.event.DeserializationEvent.class,
        jdk.internal.event.ProcessStartEvent.class,
        jdk.internal.event.SecurityPropertyModificationEvent.class,
        jdk.internal.event.TLSHandshakeEvent.class,
        jdk.internal.event.X509CertificateEvent.class,
        jdk.internal.event.X509ValidationEvent.class,

        DirectBufferStatisticsEvent.class
    };

    // This is a list of the classes with instrumentation code that should be applied.
    private static final Class<?>[] instrumentationClasses = new Class<?>[] {
        FileInputStreamInstrumentor.class,
        FileOutputStreamInstrumentor.class,
        RandomAccessFileInstrumentor.class,
        FileChannelImplInstrumentor.class,
        SocketInputStreamInstrumentor.class,
        SocketOutputStreamInstrumentor.class,
        SocketChannelImplInstrumentor.class
    };

    private static final Class<?>[] targetClasses = new Class<?>[instrumentationClasses.length];
    private static final JVM jvm = JVM.getJVM();
    private static final Runnable emitExceptionStatistics = JDKEvents::emitExceptionStatistics;
    private static final Runnable emitDirectBufferStatistics = JDKEvents::emitDirectBufferStatistics;
    private static final Runnable emitContainerConfiguration = JDKEvents::emitContainerConfiguration;
    private static final Runnable emitContainerCPUUsage = JDKEvents::emitContainerCPUUsage;
    private static final Runnable emitContainerCPUThrottling = JDKEvents::emitContainerCPUThrottling;
    private static final Runnable emitContainerMemoryUsage = JDKEvents::emitContainerMemoryUsage;
    private static final Runnable emitContainerIOUsage = JDKEvents::emitContainerIOUsage;
    private static Metrics containerMetrics = null;
    private static boolean initializationTriggered;

    @SuppressWarnings("unchecked")
    public synchronized static void initialize() {
        try {
            if (initializationTriggered == false) {
                for (Class<?> mirrorEventClass : mirrorEventClasses) {
                    SecuritySupport.registerMirror(((Class<? extends Event>)mirrorEventClass));
                }
                for (Class<?> eventClass : eventClasses) {
                    SecuritySupport.registerEvent((Class<? extends Event>) eventClass);
                }

                RequestEngine.addTrustedJDKHook(ExceptionStatisticsEvent.class, emitExceptionStatistics);
                RequestEngine.addTrustedJDKHook(DirectBufferStatisticsEvent.class, emitDirectBufferStatistics);

                initializeContainerEvents();
                initializationTriggered = true;
            }
        } catch (Exception e) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.WARN, "Could not initialize JDK events. " + e.getMessage());
        }
    }

    public static void addInstrumentation() {
        try {
            List<Class<?>> list = new ArrayList<>();
            for (int i = 0; i < instrumentationClasses.length; i++) {
                JIInstrumentationTarget tgt = instrumentationClasses[i].getAnnotation(JIInstrumentationTarget.class);
                Class<?> clazz = Class.forName(tgt.value());
                targetClasses[i] = clazz;
                list.add(clazz);
            }
            list.add(java.lang.Throwable.class);
            list.add(java.lang.Error.class);
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.INFO, "Retransformed JDK classes");
            jvm.retransformClasses(list.toArray(new Class<?>[list.size()]));
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.WARN, "Could not add instrumentation for JDK events. " + e.getMessage());
        }
    }

    private static void initializeContainerEvents() {
        containerMetrics = Container.metrics();
        SecuritySupport.registerEvent(ContainerConfigurationEvent.class);
        SecuritySupport.registerEvent(ContainerCPUUsageEvent.class);
        SecuritySupport.registerEvent(ContainerCPUThrottlingEvent.class);
        SecuritySupport.registerEvent(ContainerMemoryUsageEvent.class);
        SecuritySupport.registerEvent(ContainerIOUsageEvent.class);

        RequestEngine.addTrustedJDKHook(ContainerConfigurationEvent.class, emitContainerConfiguration);
        RequestEngine.addTrustedJDKHook(ContainerCPUUsageEvent.class, emitContainerCPUUsage);
        RequestEngine.addTrustedJDKHook(ContainerCPUThrottlingEvent.class, emitContainerCPUThrottling);
        RequestEngine.addTrustedJDKHook(ContainerMemoryUsageEvent.class, emitContainerMemoryUsage);
        RequestEngine.addTrustedJDKHook(ContainerIOUsageEvent.class, emitContainerIOUsage);
    }

    private static void emitExceptionStatistics() {
        ExceptionStatisticsEvent t = new ExceptionStatisticsEvent();
        t.throwables = ThrowableTracer.numThrowables();
        t.commit();
    }

    private static void emitContainerConfiguration() {
        if (containerMetrics != null) {
            ContainerConfigurationEvent t = new ContainerConfigurationEvent();
            t.containerType = containerMetrics.getProvider();
            t.cpuSlicePeriod = containerMetrics.getCpuPeriod();
            t.cpuQuota = containerMetrics.getCpuQuota();
            t.cpuShares = containerMetrics.getCpuShares();
            t.effectiveCpuCount = containerMetrics.getEffectiveCpuCount();
            t.memorySoftLimit = containerMetrics.getMemorySoftLimit();
            t.memoryLimit = containerMetrics.getMemoryLimit();
            t.swapMemoryLimit = containerMetrics.getMemoryAndSwapLimit();
            t.commit();
        }
    }

    private static void emitContainerCPUUsage() {
        if (containerMetrics != null) {
            ContainerCPUUsageEvent event = new ContainerCPUUsageEvent();

            event.cpuTime = containerMetrics.getCpuUsage();
            event.cpuSystemTime = containerMetrics.getCpuSystemUsage();
            event.cpuUserTime = containerMetrics.getCpuUserUsage();
            event.commit();
        }
    }
    private static void emitContainerMemoryUsage() {
        if (containerMetrics != null) {
            ContainerMemoryUsageEvent event = new ContainerMemoryUsageEvent();

            event.memoryFailCount = containerMetrics.getMemoryFailCount();
            event.memoryUsage = containerMetrics.getMemoryUsage();
            event.swapMemoryUsage = containerMetrics.getMemoryAndSwapUsage();
            event.commit();
        }
    }

    private static void emitContainerIOUsage() {
        if (containerMetrics != null) {
            ContainerIOUsageEvent event = new ContainerIOUsageEvent();

            event.serviceRequests = containerMetrics.getBlkIOServiceCount();
            event.dataTransferred = containerMetrics.getBlkIOServiced();
            event.commit();
        }
    }

    private static void emitContainerCPUThrottling() {
        if (containerMetrics != null) {
            ContainerCPUThrottlingEvent event = new ContainerCPUThrottlingEvent();

            event.cpuElapsedSlices = containerMetrics.getCpuNumPeriods();
            event.cpuThrottledSlices = containerMetrics.getCpuNumThrottled();
            event.cpuThrottledTime = containerMetrics.getCpuThrottledTime();
            event.commit();
        }
    }

    @SuppressWarnings("deprecation")
    public static byte[] retransformCallback(Class<?> klass, byte[] oldBytes) throws Throwable {
        if (java.lang.Throwable.class == klass) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.TRACE, "Instrumenting java.lang.Throwable");
            return ConstructorTracerWriter.generateBytes(java.lang.Throwable.class, oldBytes);
        }

        if (java.lang.Error.class == klass) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.TRACE, "Instrumenting java.lang.Error");
            return ConstructorTracerWriter.generateBytes(java.lang.Error.class, oldBytes);
        }

        for (int i = 0; i < targetClasses.length; i++) {
            if (targetClasses[i].equals(klass)) {
                Class<?> c = instrumentationClasses[i];
                if (Logger.shouldLog(LogTag.JFR_SYSTEM, LogLevel.TRACE)) {
                    Logger.log(LogTag.JFR_SYSTEM, LogLevel.TRACE, "Processing instrumentation class: " + c);
                }
                return new JIClassInstrumentation(instrumentationClasses[i], klass, oldBytes).getNewBytes();
            }
        }
        return oldBytes;
    }

    public static void remove() {
        RequestEngine.removeHook(emitExceptionStatistics);
        RequestEngine.removeHook(emitDirectBufferStatistics);

        RequestEngine.removeHook(emitContainerConfiguration);
        RequestEngine.removeHook(emitContainerCPUUsage);
        RequestEngine.removeHook(emitContainerCPUThrottling);
        RequestEngine.removeHook(emitContainerMemoryUsage);
        RequestEngine.removeHook(emitContainerIOUsage);
    }

    private static void emitDirectBufferStatistics() {
        DirectBufferStatisticsEvent e = new DirectBufferStatisticsEvent();
        e.commit();
    }
}
