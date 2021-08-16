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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Base class for XML-elements
class XmlElement {
    private final Map<String, String> attributes = new HashMap<>();
    private final List<XmlElement> elements = new ArrayList<>();
    private final List<XmlElement> listeners = new ArrayList<>(2);
    private final List<XmlElement> producers = new ArrayList<>(2);
    private final String elementName;

    private XmlElement parent;
    private String content = "";

    XmlElement() {
        this.elementName = Utilities.elementName(this.getClass());
    }

    private XmlElement(String elementName) {
        this.elementName = elementName;
    }

    final String getElementName() {
        return elementName;
    }

    final boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    final Map<String, String> getAttributes() {
        return attributes;
    }

    final void validate() throws ParseException {
        validateAttributes();
        validateChildConstraints();
        validateChildren();
    }

    final void setAttribute(String key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(key);
        attributes.put(key, value);
    }

    final XmlElement createChild(String name) {
        XmlElement child = instantiate(name);
        child.parent = this;
        addChild(child);
        return child;
    }

    private XmlElement instantiate(String name) {
        for (var e : constraints()) {
            String elementName = Utilities.elementName(e.type());
            if (elementName.equalsIgnoreCase(name)) {
                return Utilities.instantiate(e.type());
            }
        }
        return new XmlElement(name);
    }

    final void addChild(XmlElement element) {
        elements.add(element);
    }

    final List<XmlElement> getChildren() {
        return elements;
    }

    void setContent(String content) {
        this.content = content;
    }

    final String getContent() {
        return content;
    }

    final void addListener(XmlElement listener) {
        listeners.add(listener);
        listener.addProducer(this);
    }

    // If the element should be surrounded with an empty
    // line when printed to file, for example, <event> but not <setting>
    boolean isEntity() {
        return true;
    }

    String comment() {
        return "";
    }

    protected final void notifyListeners() {
        onChange();
        SettingsLog.flush();
    }

    protected void onChange() {
        for (XmlElement listener : listeners) {
            listener.onChange();
        }
    }

    protected final XmlElement getParent() {
        return parent;
    }

    protected List<Constraint> constraints() {
        return List.of();
    }

    protected List<String> attributes() {
        return List.of();
    }

    protected final List<XmlElement> getProducers() {
        return producers;
    }

    protected final Optional<String> optional(String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    protected final String attribute(String name) {
        return attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    protected final <T> List<T> elements(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (XmlElement e : elements) {
            if (type.isAssignableFrom(e.getClass())) {
                result.add((T) e);
            }
        }
        return result;
    }

    protected Result evaluate() {
        if (producers.isEmpty()) {
            throw new Error("No producer evaluate for " + getClass());
        }
        if (producers.size() != 1) {
            throw new Error("Unsure how to evaluate multiple producers " + getClass());
        }
        return producers.get(0).evaluate();
    }

    protected void validateAttributes() throws ParseException {
        for (String key : attributes()) {
            if (!attributes.containsKey(key)) {
                throw new ParseException("Missing mandatory attribute '" + key + "'", 0);
            }
        }
    }

    private void validateChildren() throws ParseException {
        for (XmlElement child : elements) {
            child.validate();
        }
    }

    protected void validateChildConstraints() throws ParseException {
        for (Constraint c : constraints()) {
            validateConstraint(c);
        }
    }

    private final void validateConstraint(Constraint c) throws ParseException {
        int count = count(c.type());
        if (count < c.min()) {
            String elementName = Utilities.elementName(c.type());
            throw new ParseException("Missing mandatory element <" + elementName + ">", 0);
        }
        if (count > c.max()) {
            String elementName = Utilities.elementName(c.type());
            throw new ParseException("Too many elements of type <" + elementName + ">", 0);
        }
    }

    private void addProducer(XmlElement producer) {
        producers.add(producer);
    }

    private int count(Class<? extends XmlElement> type) {
        int count = 0;
        for (XmlElement element : getChildren()) {
            if (type.isAssignableFrom(element.getClass())) {
                count++;
            }
        }
        return count;
    }
}
