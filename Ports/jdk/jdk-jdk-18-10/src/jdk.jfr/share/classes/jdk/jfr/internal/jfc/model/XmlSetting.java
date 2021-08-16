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

import java.util.List;
import java.util.Optional;

// Corresponds to <setting>
final class XmlSetting extends XmlElement {

    @Override
    public boolean isEntity() {
        return false;
    }

    @Override
    protected List<String> attributes() {
        return List.of("name");
    }

    public String getName() {
        return attribute("name");
    }

    public Optional<String> getControl() {
        return optional("control");
    }

    @Override
    public void onChange() {
        String value = evaluate().value();
        if (value != null) {
            setContent(value);
        }
    }

    @Override
    final void setContent(String value) {
        super.setContent(value);
        if (getParent() instanceof XmlEvent event) {
            SettingsLog.log(this, value);
        }
    }

    @Override
    protected Result evaluate() {
        for (XmlElement producer : getProducers()) {
            Result result = producer.evaluate();
            if (!result.isNull()) {
                return result;
            }
        }
        return Result.NULL;
    }

    public String getFullName() {
        if (getParent() instanceof XmlEvent event) {
            return event.getName() + "#" + getName();
        }
        return "unknown";
    }
}
