/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8131029 8160127 8159935 8168615
 * @summary Test that fail-over works for fail-over ExecutionControl generators.
 * @modules jdk.jshell/jdk.jshell.execution
 *          jdk.jshell/jdk.jshell.spi
 * @build KullaTesting ExecutionControlTestBase
 * @run testng FailOverExecutionControlTest
 */

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

@Test
public class FailOverExecutionControlTest extends ExecutionControlTestBase {

    @BeforeMethod
    @Override
    public void setUp() {
        setUp(builder -> builder.executionEngine("failover:0(expectedFailureNonExistent1), 1(expectedFailureNonExistent2), "
                + standardSpecs()));
    }

}
