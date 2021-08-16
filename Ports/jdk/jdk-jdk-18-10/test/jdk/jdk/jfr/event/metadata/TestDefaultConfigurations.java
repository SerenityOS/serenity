/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.metadata;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.SettingDescriptor;
import jdk.test.lib.jfr.EventNames;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 *
 * @library /test/lib
 * @modules java.xml
 *          jdk.jfr
 *
 * @run main/othervm jdk.jfr.event.metadata.TestDefaultConfigurations
 */
public class TestDefaultConfigurations {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static void main(String[] args) throws Exception {
        List<String> errors = new ArrayList<>();

        errors.addAll(testConfiguration(Configuration.getConfiguration("default")));
        errors.addAll(testConfiguration(Configuration.getConfiguration("profile")));

        if (!errors.isEmpty()) {
            throwExceptionWithErrors(errors);
        }
    }

    private static List<String> testConfiguration(Configuration config) throws ParserConfigurationException, SAXException, IOException {
        List<String> errors = new ArrayList<>();

        Map<String, EventType> eventTypeLookup = new HashMap<>();
        for (EventType t : FlightRecorder.getFlightRecorder().getEventTypes()) {
            eventTypeLookup.put(t.getName(), t);
        }
        String content = config.getContents();
        Document doc = createDocument(content);
        Element configuration = doc.getDocumentElement();
        errors.addAll(checkConfiguration(configuration));
        for (Element event : getChildElements(configuration, "event")) {
            String name = event.getAttribute("name");

            EventType cd = eventTypeLookup.get(name);
            if (cd != null) {
                errors.addAll(checkSettings(config, cd, event));
            } else {
                errors.add("Preset '" + config.getName() + "' reference unknown event '" + name + "'");
            }
            eventTypeLookup.remove(name);
        }
        for (String name : eventTypeLookup.keySet()) {
            errors.add("Preset '" + config.getName() + "' doesn't configure event '" + name + "'");
        }

        return errors;
    }

    private static void throwExceptionWithErrors(List<String> errors) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String error : errors) {
            sb.append(error);
            sb.append(LINE_SEPARATOR);
        }
        throw new Exception(sb.toString());
    }

    private static List<String> checkConfiguration(Element configuration) {
        List<String> errors = new ArrayList<>();
        if (configuration.getAttribute("description").length() < 2) {
            errors.add("Configuration should have a valid description!");
        }
        if (configuration.getAttribute("label").length() < 2) {
            errors.add("Configuration should have a label!");
        }
        if (!configuration.getAttribute("provider").equals("Oracle")) {
            errors.add("Configuration should set provider to 'Oracle'!");
        }
        return errors;
    }

    private static List<String> checkSettings(Configuration config, EventType eventType, Element event) {
        List<String> errors = new ArrayList<>();

        Set<String> requiredSettings = createRequiredSettingNameSet(eventType);
        for (Element setting : getChildElements(event, "setting")) {
            String settingName = setting.getAttribute("name");
            if (requiredSettings.contains(settingName)) {
                requiredSettings.remove(settingName);
            } else {
                errors.add("Setting '" + settingName + "' for event '" + eventType.getName() + "' should not be part of confirguaration '" + config.getName()
                        + "' since it won't have an impact on the event.");
            }
        }
        for (String required : requiredSettings) {
            errors.add("Setting '" + required + "' in event '" + eventType.getName() + "' was not configured in the configuration '" + config.getName() + "'");
        }

        return errors;
    }

    private static Set<String> createRequiredSettingNameSet(EventType cd) {
        Set<String> requiredSettings = new HashSet<>();
        for (SettingDescriptor s : cd.getSettingDescriptors()) {
            requiredSettings.add(s.getName());
        }
        return requiredSettings;
    }

    private static Document createDocument(String content) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new StringReader(content)));
        doc.getDocumentElement().normalize();
        // Don't want to add these settings to the jfc-files we ship since they
        // are not useful to configure. They are however needed to make the test
        // pass.
        insertSetting(doc, EventNames.ActiveSetting, "stackTrace", "false");
        insertSetting(doc, EventNames.ActiveSetting, "threshold", "0 ns");
        insertSetting(doc, EventNames.ActiveRecording, "stackTrace", "false");
        insertSetting(doc, EventNames.ActiveRecording, "threshold", "0 ns");
        insertSetting(doc, EventNames.JavaExceptionThrow, "threshold", "0 ns");
        insertSetting(doc, EventNames.JavaErrorThrow, "threshold", "0 ns");
        insertSetting(doc, EventNames.SecurityProperty, "threshold", "0 ns");
        insertSetting(doc, EventNames.TLSHandshake, "threshold", "0 ns");
        insertSetting(doc, EventNames.X509Certificate, "threshold", "0 ns");
        insertSetting(doc, EventNames.X509Validation, "threshold", "0 ns");
        insertSetting(doc, EventNames.ProcessStart, "threshold", "0 ns");
        insertSetting(doc, EventNames.Deserialization, "threshold", "0 ns");

        return doc;
    }

    private static void insertSetting(Document doc, String eventName, String settingName, String settingValue) {
        for (Element event : getChildElements(doc.getDocumentElement(), "event")) {
            Attr attribute = event.getAttributeNode("name");
            if (attribute != null) {
                if (eventName.equals(attribute.getValue())) {
                    Element setting = doc.createElement("setting");
                    setting.setAttribute("name", settingName);
                    setting.setTextContent(settingValue);
                    event.appendChild(setting);
                }
            }
        }
    }

    private static Collection<Element> getChildElements(Element parent, String name) {
        NodeList elementsByTagName = parent.getElementsByTagName(name);
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            Node node = elementsByTagName.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }
}
