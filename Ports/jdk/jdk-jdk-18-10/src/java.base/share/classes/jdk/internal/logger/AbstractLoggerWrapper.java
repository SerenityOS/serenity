/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.logger;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import sun.util.logging.PlatformLogger;

/**
 * An implementation of {@link System.Logger System.Logger}
 * that redirects all calls to a wrapped instance of {@link
 * System.Logger System.Logger}
 *
 * @param <L> Type of the wrapped Logger: {@code Logger} or
 *        an extension of that interface.
 *
 */
abstract class AbstractLoggerWrapper<L extends Logger>
    implements Logger, PlatformLogger.Bridge, PlatformLogger.ConfigurableBridge {

    AbstractLoggerWrapper() { }

    abstract L wrapped();

    abstract PlatformLogger.Bridge platformProxy();

    L getWrapped() {
        return wrapped();
    }

    @Override
    public final String getName() {
        return wrapped().getName();
    }

    // -----------------------------------------------------------------
    // Generic methods taking a Level as parameter
    // -----------------------------------------------------------------


    @Override
    public boolean isLoggable(Level level) {
        return wrapped().isLoggable(level);
    }

    @Override
    public void log(Level level, String msg) {
        wrapped().log(level, msg);
    }

    @Override
    public void log(Level level,
                    Supplier<String> msgSupplier) {
        wrapped().log(level, msgSupplier);
    }

    @Override
    public void log(Level level, Object obj) {
        wrapped().log(level, obj);
    }

    @Override
    public void log(Level level,
                   String msg, Throwable thrown) {
        wrapped().log(level, msg, thrown);
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
        wrapped().log(level, msgSupplier, thrown);
    }

    @Override
    public void log(Level level,
                    String format, Object... params) {
        wrapped().log(level, format, params);
    }

    @Override
    public void log(Level level, ResourceBundle bundle,
                    String key, Throwable thrown) {
        wrapped().log(level, bundle, key, thrown);
    }

    @Override
    public void log(Level level, ResourceBundle bundle,
                    String format, Object... params) {
        wrapped().log(level, bundle, format, params);
    }

    // ---------------------------------------------------------
    // Methods from PlatformLogger.Bridge
    // ---------------------------------------------------------

    @Override
    public boolean isLoggable(PlatformLogger.Level level) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null) return isLoggable(level.systemLevel());
        else return platformProxy.isLoggable(level);
    }

    @Override
    public boolean isEnabled() {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        return platformProxy == null || platformProxy.isEnabled();
    }

    @Override
    public void log(PlatformLogger.Level level, String msg) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(), msg);
        } else {
            platformProxy.log(level, msg);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg, Throwable thrown) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(), msg, thrown);
        } else {
            platformProxy.log(level, msg, thrown);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg, Object... params) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(), msg, params);
        } else {
            platformProxy.log(level, msg, params);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, Supplier<String> msgSupplier) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(),msgSupplier);
        } else {
            platformProxy.log(level,msgSupplier);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, Throwable thrown,
                    Supplier<String> msgSupplier) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(), msgSupplier, thrown);
        } else {
            platformProxy.log(level, thrown, msgSupplier);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
                     String sourceMethod, String msg) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            if (sourceClass == null && sourceMethod == null) { // best effort
                wrapped().log(level.systemLevel(), msg);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    sourceClass  = sourceClass  == null ? "" : sourceClass;
                    sourceMethod = sourceMethod == null ? "" : sourceMethod;
                    msg = msg == null ? "" : msg;
                    wrapped.log(systemLevel, String.format("[%s %s] %s",
                            sourceClass, sourceMethod, msg));
                }
            }
        } else {
            platformProxy.logp(level, sourceClass, sourceMethod, msg);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
                     String sourceMethod, Supplier<String> msgSupplier) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null) { // best effort
            if (sourceClass == null && sourceMethod == null) {
                wrapped().log(level.systemLevel(), msgSupplier);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    final String sClass  = sourceClass  == null ? "" : sourceClass;
                    final String sMethod = sourceMethod == null ? "" : sourceMethod;
                    wrapped.log(systemLevel, () -> String.format("[%s %s] %s",
                            sClass, sMethod, msgSupplier.get()));
                }
            }
        } else {
            platformProxy.logp(level, sourceClass, sourceMethod, msgSupplier);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
                     String sourceMethod, String msg, Object... params) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null) { // best effort
            if (sourceClass == null && sourceMethod == null) {
                wrapped().log(level.systemLevel(), msg, params);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    sourceClass  = sourceClass  == null ? "" : sourceClass;
                    sourceMethod = sourceMethod == null ? "" : sourceMethod;
                    msg = msg == null ? "" : msg;
                    wrapped.log(systemLevel, String.format("[%s %s] %s",
                            sourceClass, sourceMethod, msg), params);
                }
            }
        } else {
            platformProxy.logp(level, sourceClass, sourceMethod, msg, params);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
                     String sourceMethod, String msg, Throwable thrown) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null) { // best effort
            if (sourceClass == null && sourceMethod == null) {
                wrapped().log(level.systemLevel(), msg, thrown);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    sourceClass  = sourceClass  == null ? "" : sourceClass;
                    sourceMethod = sourceMethod == null ? "" : sourceMethod;
                    msg = msg == null ? "" : msg;
                    wrapped.log(systemLevel, String.format("[%s %s] %s",
                            sourceClass, sourceMethod, msg), thrown);
                }
            }
        } else {
            platformProxy.logp(level, sourceClass, sourceMethod, msg, thrown);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
                     String sourceMethod, Throwable thrown,
                     Supplier<String> msgSupplier) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  { // best effort
            if (sourceClass == null && sourceMethod == null) {
                wrapped().log(level.systemLevel(), msgSupplier, thrown);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    final String sClass  = sourceClass  == null ? "" : sourceClass;
                    final String sMethod = sourceMethod == null ? "" : sourceMethod;
                    wrapped.log(systemLevel,  () -> String.format("[%s %s] %s",
                            sClass, sMethod, msgSupplier.get()), thrown);
                }
            }
        } else {
            platformProxy.logp(level, sourceClass, sourceMethod,
                               thrown, msgSupplier);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, String sourceClass,
                      String sourceMethod, ResourceBundle bundle,
                      String msg, Object... params) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  { // best effort
            if (bundle != null || sourceClass == null && sourceMethod == null) {
                wrapped().log(level.systemLevel(), bundle, msg, params);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    sourceClass  = sourceClass  == null ? "" : sourceClass;
                    sourceMethod = sourceMethod == null ? "" : sourceMethod;
                    msg = msg == null ? "" : msg;
                    wrapped.log(systemLevel, bundle, String.format("[%s %s] %s",
                            sourceClass, sourceMethod, msg), params);
                }
            }
        } else {
            platformProxy.logrb(level, sourceClass, sourceMethod,
                    bundle, msg, params);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, String sourceClass,
                      String sourceMethod, ResourceBundle bundle, String msg,
                      Throwable thrown) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  { // best effort
            if (bundle != null || sourceClass == null && sourceMethod == null) {
                wrapped().log(level.systemLevel(), bundle, msg, thrown);
            } else {
                Level systemLevel = level.systemLevel();
                Logger wrapped = wrapped();
                if (wrapped.isLoggable(systemLevel)) {
                    sourceClass  = sourceClass  == null ? "" : sourceClass;
                    sourceMethod = sourceMethod == null ? "" : sourceMethod;
                    msg = msg == null ? "" : msg;
                    wrapped.log(systemLevel, bundle, String.format("[%s %s] %s",
                            sourceClass, sourceMethod, msg), thrown);
                }
            }
        } else {
            platformProxy.logrb(level, sourceClass, sourceMethod, bundle,
                                msg, thrown);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, ResourceBundle bundle,
                      String msg, Throwable thrown) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(), bundle, msg, thrown);
        } else {
            platformProxy.logrb(level, bundle, msg, thrown);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, ResourceBundle bundle,
                      String msg, Object... params) {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        if (platformProxy == null)  {
            wrapped().log(level.systemLevel(), bundle, msg, params);
        } else {
            platformProxy.logrb(level, bundle, msg, params);
        }
    }


    @Override
    public LoggerConfiguration getLoggerConfiguration() {
        final PlatformLogger.Bridge platformProxy = platformProxy();
        return platformProxy == null ? null
               : PlatformLogger.ConfigurableBridge
                       .getLoggerConfiguration(platformProxy);
    }

}
