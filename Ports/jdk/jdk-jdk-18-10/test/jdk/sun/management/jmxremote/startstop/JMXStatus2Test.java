/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A flavor of {@linkplain JMXStatusTest} test where the test application
 * is started with the management agent initialized.
  */
public class JMXStatus2Test extends JMXStatusTest {
    @Override
    protected List<String> getCustomVmArgs() {
        // not specifying the jmxremote.port > only local agent started
        return Arrays.asList(
            "-Dcom.sun.management.jmxremote"
        );
    }

    @Override
    protected Pattern getDefaultPattern() {
        return JMXStatusTest.LOCAL_AGENT_STATUS;
    }
}