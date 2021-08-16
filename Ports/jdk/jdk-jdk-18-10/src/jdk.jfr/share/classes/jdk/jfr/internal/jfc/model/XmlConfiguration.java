/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.jfc.model;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

// Corresponds to <configuration>
final class XmlConfiguration extends XmlElement {

    public List<XmlEvent> getEvents() {
        return elements(XmlEvent.class);
    }

    public Optional<String> getDescription() {
        return optional("description");
    }

    public Optional<String> getLabel() {
        return optional("label");
    }

    public Optional<String> getProvider() {
        return optional("provider");
    }

    public Optional<String> getVersion() {
        return optional("version");
    }

    public List<XmlControl> getControls() {
        return elements(XmlControl.class);
    }

    @Override
    String comment() {
        return """
               Recommended way to edit .jfc files is to use the configure command of
               the 'jfr' tool, i.e. jfr configure, or JDK Mission Control
               see Window -> Flight Recorder Template Manager
               """;
    }

    XmlEvent getEvent(String eventName, boolean add) {
        for (XmlEvent event : getEvents()) {
            if (eventName.equals(event.getName())) {
                return event;
            }
        }
        if (!add) {
            throw new IllegalArgumentException("Could not find event '" + eventName + "'");
        }
        XmlEvent event = new XmlEvent();
        event.setAttribute("name", eventName);
        addChild(event);
        return event;
    }

    @Override
    protected List<String> attributes() {
        return List.of("version", "label");
    }

    @Override
    protected void validateAttributes() throws ParseException {
        super.validateAttributes();
        if (!attribute("version").equals("2.0")) {
            throw new ParseException("Only .jfc files of version 2.0 is supported", -1);
        }
    }

    @Override
    protected List<Constraint> constraints() {
        return List.of(
            Constraint.any(XmlEvent.class),
            Constraint.any(XmlControl.class)
        );
    }
}
