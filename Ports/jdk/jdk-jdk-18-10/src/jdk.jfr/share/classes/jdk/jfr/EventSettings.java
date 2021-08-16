/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr;

import java.time.Duration;
import java.util.Map;

import jdk.jfr.internal.management.EventSettingsModifier;

/**
 * Convenience class for applying event settings to a recording.
 * <p>
 * An {@code EventSettings} object for a recording can be obtained by invoking
 * the {@link Recording#enable(String)} method which is configured using method
 * chaining.
 * <p>
 * The following example shows how to use the {@code EventSettings} class.
 * <pre>
 * {@code
 * Recording r = new Recording();
 * r.enable("jdk.CPULoad")
 *    .withPeriod(Duration.ofSeconds(1));
 * r.enable("jdk.FileWrite")
 *    .withoutStackTrace()
 *    .withThreshold(Duration.ofNanos(10));
 * r.start();
 * Thread.sleep(10_000);
 * r.stop();
 * r.dump(Files.createTempFile("recording", ".jfr"));
 *
 * }
 * </pre>
 * @since 9
 */
public abstract class EventSettings {

    // Used to provide EventSettings for jdk.management.jfr module
    static class DelegatedEventSettings extends EventSettings {
        private final EventSettingsModifier delegate;

        DelegatedEventSettings(EventSettingsModifier modifier) {
            this.delegate = modifier;
        }

        @Override
        public EventSettings with(String name, String value) {
             delegate.with(name, value);
             return this;
        }

        @Override
        Map<String, String> toMap() {
            return delegate.toMap();
        }
    }

    // package private
    EventSettings() {
    }

    /**
     * Enables stack traces for the event that is associated with this event setting.
     * <p>
     * Equivalent to invoking the {@code with("stackTrace", "true")} method.
     *
     * @return event settings object for further configuration, not {@code null}
     */
    public final EventSettings withStackTrace() {
        return with(StackTrace.NAME, "true");
    }

    /**
     * Disables stack traces for the event that is associated with this event setting.
     * <p>
     * Equivalent to invoking the {@code with("stackTrace", "false")} method.
     *
     * @return event settings object for further configuration, not {@code null}
     */
    public final EventSettings withoutStackTrace() {
        return with(StackTrace.NAME, "false");
    }

    /**
     * Specifies that a threshold is not used.
     * <p>
     * This is a convenience method, equivalent to invoking the
     * {@code with("threshold", "0 s")} method.
     *
     * @return event settings object for further configuration, not {@code null}
     */
    public final EventSettings withoutThreshold() {
        return with(Threshold.NAME, "0 s");
    }

    /**
     * Sets the interval for the event that is associated with this event setting.
     *
     * @param duration the duration, not {@code null}
     *
     * @return event settings object for further configuration, not {@code null}
     */
    public final EventSettings withPeriod(Duration duration) {
        return with(Period.NAME, duration.toNanos() + " ns");
    }

    /**
     * Sets the threshold for the event that is associated with this event setting.
     *
     * @param duration the duration, or {@code null} if no duration is used
     *
     * @return event settings object for further configuration, not {@code null}
     */
    public final EventSettings withThreshold(Duration duration) {
        if (duration == null) {
            return with(Threshold.NAME, "0 ns");
        } else {
            return with(Threshold.NAME, duration.toNanos() + " ns");
        }
    }

    /**
     * Sets a setting value for the event that is associated with this event setting.
     *
     * @param name the name of the setting (for example, {@code "threshold"})
     *
     * @param value the value to set (for example {@code "20 ms"} not
     *        {@code null})
     *
     * @return event settings object for further configuration, not {@code null}
     */
    public abstract EventSettings with(String name, String value);

    /**
     * Creates a settings {@code Map} for the event that is associated with this
     * event setting.
     *
     * @return a settings {@code Map}, not {@code null}
     */
    abstract Map<String, String> toMap();
}
