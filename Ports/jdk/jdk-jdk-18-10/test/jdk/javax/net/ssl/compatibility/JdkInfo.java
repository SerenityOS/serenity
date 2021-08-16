/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * It represents a JDK with some specific attributes.
 * If two JdkInfo instances have the same version value, the instances are
 * regarded as equivalent.
 */
public class JdkInfo {

    public static final JdkInfo DEFAULT = new JdkInfo(Jdk.DEFAULT.getPath());

    public final Path javaPath;

    public final String version;
    public final List<String> supportedProtocols;
    public final List<String> enabledProtocols;
    public final List<String> supportedCipherSuites;
    public final List<String> enabledCipherSuites;
    public final boolean supportsSNI;
    public final boolean supportsALPN;

    public JdkInfo(Path javaPath) {
        this.javaPath = javaPath;

        String output = jdkAttributes(javaPath);
        if (output == null || output.trim().isEmpty()) {
            throw new RuntimeException(
                    "Cannot determine the JDK attributes: " + javaPath);
        }

        String[] attributes = Utilities.split(output, Utilities.PARAM_DELIMITER);
        version = parseAttribute(attributes[0]);
        supportedProtocols = parseListAttribute(attributes[1]);
        enabledProtocols = parseListAttribute(attributes[2]);
        supportedCipherSuites = parseListAttribute(attributes[3]);
        enabledCipherSuites = parseListAttribute(attributes[4]);
        supportsSNI = parseBooleanAttribute(attributes[5]);
        supportsALPN = parseBooleanAttribute(attributes[6]);
    }

    private List<String> parseListAttribute(String attribute) {
        attribute = parseAttribute(attribute);
        return Arrays.asList(attribute.split(","));
    }

    private boolean parseBooleanAttribute(String attribute) {
        attribute = parseAttribute(attribute);
        return Boolean.parseBoolean(attribute);
    }
    private String parseAttribute(String attribute) {
        return attribute.replaceAll(".*=", "");
    }

    // Determines the specific attributes for the specified JDK.
    private static String jdkAttributes(Path javaPath) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("java.security.properties", Utils.SEC_PROPS_FILE);
        return ProcUtils.java(javaPath, JdkInfoUtils.class, props).getOutput();
    }

    @Override
    public int hashCode() {
        return version == null ? 0 : version.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JdkInfo other = (JdkInfo) obj;
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    public boolean supportsProtocol(Protocol protocol) {
        return supportedProtocols.contains(protocol.name);
    }

    public boolean enablesProtocol(Protocol protocol) {
        return enabledProtocols.contains(protocol.name);
    }

    public boolean supportsCipherSuite(CipherSuite cipherSuite) {
        return supportedCipherSuites.contains(cipherSuite.name());
    }

    public boolean enablesCipherSuite(CipherSuite cipherSuite) {
        return enabledCipherSuites.contains(cipherSuite.name());
    }
}
