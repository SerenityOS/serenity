/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.cldrconverter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.stream.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class MetaZonesParseHandler extends AbstractLDMLHandler<String> {
    final static String NO_METAZONE_KEY = "no.metazone.defined";
    final static DateTimeFormatter MZ_TIME = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendPattern("[ HH[:mm[:ss]]]")
            .toFormatter()
            .withResolverStyle(ResolverStyle.LENIENT);

    private String tzid, metazone;

    // for java.time.format.ZoneNames.java
    private List<String> mzoneMapEntryList = new ArrayList<>();
    private Map<String, String> zones = new HashMap<>();

    MetaZonesParseHandler() {
    }

    @Override
    public InputSource resolveEntity(String publicID, String systemID) throws IOException, SAXException {
        // avoid HTTP traffic to unicode.org
        if (systemID.startsWith(CLDRConverter.SPPL_LDML_DTD_SYSTEM_ID)) {
            return new InputSource((new File(CLDRConverter.LOCAL_SPPL_LDML_DTD)).toURI().toString());
        }
        return null;
    }

    // metaZone: ID -> metazone
    // per locale: ID -> names, metazone -> names
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
        case "timezone":
            tzid = attributes.getValue("type");
            pushContainer(qName, attributes);
            break;

        case "usesMetazone":
            // uses the time of the JDK build to determine metazones.
            String from = attributes.getValue("from");
            String to = attributes.getValue("to");
            LocalDateTime fromLDT = from != null ? MZ_TIME.parse(from, LocalDateTime::from) : LocalDateTime.MIN;
            LocalDateTime toLDT = to != null ? MZ_TIME.parse(to, LocalDateTime::from) : LocalDateTime.MAX;
            LocalDateTime now = LocalDateTime.now();

            if (fromLDT.isBefore(now) && toLDT.isAfter(now)) {
                metazone = attributes.getValue("mzone");
            }
            pushIgnoredContainer(qName);
            break;

        case "mapZone":
            String territory = attributes.getValue("territory");
            if (territory.equals("001")) {
                zones.put(attributes.getValue("other"), attributes.getValue("type"));
            } else {
                mzoneMapEntryList.add(String.format("        \"%s\", \"%s\", \"%s\",",
                    attributes.getValue("other"),
                    territory,
                    attributes.getValue("type")));
            }
            pushIgnoredContainer(qName);
            break;

        case "version":
        case "generation":
            pushIgnoredContainer(qName);
            break;

        default:
            // treat anything else as a container
            pushContainer(qName, attributes);
            break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        assert qName.equals(currentContainer.getqName()) : "current=" + currentContainer.getqName() + ", param=" + qName;
        switch (qName) {
        case "timezone":
            if (tzid == null) {
                throw new InternalError();
            } else if (metazone == null) {
                String no_meta = get(NO_METAZONE_KEY);
                put(NO_METAZONE_KEY, no_meta == null ? tzid : no_meta + " " + tzid);
                CLDRConverter.info("No metazone defined for %s%n", tzid);
            } else {
                put(tzid, metazone);
            }
            tzid = null;
            metazone = null;
            break;
        }
        currentContainer = currentContainer.getParent();
    }

    public Map<String, String> zidMap() {
        return zones;
    }

    public Stream<String> mzoneMapEntry() {
        return mzoneMapEntryList.stream();
    }
}
