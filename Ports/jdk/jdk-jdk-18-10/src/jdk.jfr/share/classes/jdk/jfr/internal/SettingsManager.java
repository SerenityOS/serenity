/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import jdk.jfr.internal.EventControl.NamedControl;
import jdk.jfr.internal.handlers.EventHandler;

final class SettingsManager {

    private static class InternalSetting {

        private final String identifier;
        private Map<String, Set<String>> enabledMap = new LinkedHashMap<>(5);
        private Map<String, Set<String>> allMap = new LinkedHashMap<>(5);
        private boolean enabled;

        /**
         * Settings identifier, for example "com.example.HelloWorld" or "56"
         * (id of event)
         *
         * @param settingsId
         */
        public InternalSetting(String settingsId) {
            this.identifier = settingsId;
        }

        public Set<String> getValues(String key) {
            if (enabled) {
                return enabledMap.get(key);
            } else {
                return allMap.get(key);
            }
        }

        public void add(String attribute, String value) {
            if ("enabled".equals(attribute) && "true".equals(value)) {
                enabled = true;
                allMap = null; // no need to keep these around
            }
            addToMap(enabledMap, attribute, value);
            if (allMap != null) {
                addToMap(allMap, attribute, value);
            }
        }

        private void addToMap(Map<String, Set<String>> map, String attribute, String value) {
            Set<String> values = map.get(attribute);
            if (values == null) {
                values = new HashSet<String>(5);
                map.put(attribute, values);
            }
            values.add(value);

        }

        public String getSettingsId() {
            return identifier;
        }

        public void add(InternalSetting enabled) {
            for (Map.Entry<String, Set<String>> entry : enabled.enabledMap.entrySet()) {
                for (String value : entry.getValue()) {
                    add(entry.getKey(), value);
                }
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(identifier);
            sb.append(": ");
            sb.append(enabledMap.toString());
            return sb.toString();
        }

        public void finish() {
            if (!enabled) {
                // settings from disabled
                // events should not impact results, but
                // we can't clear enabledMap since enabled=false
                // needs be there, so events that are enabled
                // by default are turned off
                Map<String, Set<String>> disabledMap = new HashMap<>(2);
                Set<String> values = new HashSet<>(2);
                values.add("false");
                disabledMap.put("enabled", values);
                enabledMap = disabledMap;
            }
        }
    }

   private Map<String, InternalSetting> availableSettings = new LinkedHashMap<>();

    void setSettings(List<Map<String, String>> activeSettings) {
        // store settings so they are available if a new event class is loaded
        availableSettings = createSettingsMap(activeSettings);
        List<EventControl> eventControls = MetadataRepository.getInstance().getEventControls();
        if (!JVM.getJVM().isRecording()) {
            for (EventControl ec : eventControls) {
                ec.disable();
            }
        } else {
            if (Logger.shouldLog(LogTag.JFR_SETTING, LogLevel.INFO)) {
                Collections.sort(eventControls, (x,y) -> x.getEventType().getName().compareTo(y.getEventType().getName()));
            }
            for (EventControl ec : eventControls) {
                setEventControl(ec);
            }
        }
        if (JVM.getJVM().getAllowedToDoEventRetransforms()) {
            updateRetransform(JVM.getJVM().getAllEventClasses());
        }
    }

    public void updateRetransform(List<Class<? extends jdk.internal.event.Event>> eventClasses) {
        List<Class<?>> classes = new ArrayList<>();
        for(Class<? extends jdk.internal.event.Event> eventClass: eventClasses) {
            EventHandler eh = Utils.getHandler(eventClass);
            if (eh != null ) {
                PlatformEventType eventType = eh.getPlatformEventType();
                if (eventType.isMarkedForInstrumentation()) {
                    classes.add(eventClass);
                    eventType.markForInstrumentation(false);
                    // A bit premature to set it here, but hard to check
                    // after call to retransformClasses.
                    eventType.setInstrumented();
                }
            }
        }
        if (!classes.isEmpty()) {
            JVM.getJVM().retransformClasses(classes.toArray(new Class<?>[0]));
        }
    }

    private Map<String, InternalSetting> createSettingsMap(List<Map<String,String>> activeSettings) {
        Map<String, InternalSetting> map = new LinkedHashMap<>(activeSettings.size());
        for (Map<String, String> rec : activeSettings) {
            for (InternalSetting internal : makeInternalSettings(rec)) {
                InternalSetting is = map.get(internal.getSettingsId());
                if (is == null) {
                    map.put(internal.getSettingsId(), internal);
                } else {
                    is.add(internal);
                }
            }
        }
        return map;
    }

    private Collection<InternalSetting> makeInternalSettings(Map<String, String> rec) {
        Map<String, InternalSetting> internals = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rec.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            int index = key.indexOf("#");
            if (index > 1 && index < key.length() - 2) {
                String eventName = key.substring(0, index);
                eventName = Utils.upgradeLegacyJDKEvent(eventName);
                InternalSetting s = internals.get(eventName);
                String settingName = key.substring(index + 1).trim();
                if (s == null) {
                    s = new InternalSetting(eventName);
                    internals.put(eventName, s);
                }
                s.add(settingName, value);
            }
        }
      for (InternalSetting s : internals.values()) {
         s.finish();
      }

      return internals.values();
    }

    void setEventControl(EventControl ec) {
        InternalSetting is = getInternalSetting(ec);
        boolean shouldLog = Logger.shouldLog(LogTag.JFR_SETTING, LogLevel.INFO);
        if (shouldLog) {
            Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, "Applied settings for " + ec.getEventType().getLogName() + " {");
        }
        for (NamedControl nc: ec.getNamedControls()) {
            Set<String> values = null;
            String settingName = nc.name;
            if (is != null) {
                values = is.getValues(settingName);
            }
            Control control = nc.control;
            if (values != null) {
                control.apply(values);
                String after = control.getLastValue();
                if (shouldLog) {
                    if (Utils.isSettingVisible(control, ec.getEventType().hasEventHook())) {
                        if (values.size() > 1) {
                            StringJoiner sj = new StringJoiner(", ", "{", "}");
                            for (String s : values) {
                                sj.add("\"" + s + "\"");
                            }
                            String message = "  " + settingName + "= " + sj.toString() + " => \"" + after + "\"";
                            Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, message);
                        } else {
                            String message = "  " + settingName + "=\"" + control.getLastValue() + "\"";
                            Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, message);
                        }
                    }
                }
            } else {
                control.setDefault();
                if (shouldLog) {
                    String message = "  " + settingName + "=\"" + control.getLastValue() + "\"";
                    Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, message);
                }
            }
        }
        ec.writeActiveSettingEvent();
        if (shouldLog) {
            Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, "}");
        }
    }

    private InternalSetting getInternalSetting(EventControl ec) {
        String name = ec.getEventType().getName();
        InternalSetting nameBased = availableSettings.get(name);
        InternalSetting idBased = availableSettings.get(ec.getSettingsId());

        if (nameBased == null && idBased == null) {
            return null;
        }
        if (idBased == null) {
            return nameBased;
        }
        if (nameBased == null) {
            return idBased;
        }
        InternalSetting mixed = new InternalSetting(nameBased.getSettingsId());
        mixed.add(nameBased);
        mixed.add(idBased);
        return mixed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (InternalSetting enabled : availableSettings.values()) {
            sb.append(enabled.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    boolean isEnabled(String eventName) {
        InternalSetting is = availableSettings.get(eventName);
        if (is == null) {
            return false;
        }
        return is.isEnabled();
    }
}
