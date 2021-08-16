/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.metadata.settingdescriptor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import jdk.jfr.Description;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.SettingDefinition;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.Timespan;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.jfr.Events;

final class CustomEvent extends BaseEvent {

    public static final String DESCRIPTION_OF_AN_ANNOTATED_METHOD = "Description of an annotated method";
    public static final String ANNOTATED_METHOD = "Annotated Method";

    // should be shadowed by built-in setting threshold
    @SettingDefinition
    boolean threshold(PlainSetting p) {
        return false;
    }

    @SettingDefinition
    private boolean plain(PlainSetting s) {
        return true;
    }

    @SettingDefinition
    protected boolean annotatedType(AnnotatedSetting s) {
        return true;
    }

    @SettingDefinition
    @Name("newName")
    @Label(ANNOTATED_METHOD)
    @Description(DESCRIPTION_OF_AN_ANNOTATED_METHOD)
    @Timespan(Timespan.NANOSECONDS)
    public boolean whatever(AnnotatedSetting s) {
        return true;
    }

    @SettingDefinition
    boolean overridden(PlainSetting s) {
        return true;
    }

    public static void assertOnDisk(Comparator<SettingDescriptor> c) throws Exception {
        EventType in = EventType.getEventType(CustomEvent.class);
        Path p = Paths.get("custom.jfr");
        try (Recording r = new Recording()) {
            r.start();
            r.stop();
            r.dump(p);
        }
        try (RecordingFile f = new RecordingFile(p)) {
            for (EventType out : f.readEventTypes()) {
                if (out.getName().equals(CustomEvent.class.getName())) {
                    if (out.getSettingDescriptors().size() != in.getSettingDescriptors().size()) {
                        throw new Exception("Number of settings doesn't match");
                    }
                    for (SettingDescriptor os : out.getSettingDescriptors()) {
                        SettingDescriptor is = Events.getSetting(in, os.getName());
                        if (c.compare(os, is) != 0) {
                            throw new Exception("Setting with name " + is.getName() + " doesn't match");
                        }
                    }
                    return;
                }
            }
        }
        throw new Exception("Could not event type with name " + CustomEvent.class.getName());
    }
}
