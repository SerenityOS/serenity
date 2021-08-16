/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key jfr
 * @summary Tests emitting events in a dynamically loaded Java agent
 * @requires vm.hasJFR
 *
 * @library /test/lib /test/jdk
 * @modules java.instrument
 *
 * @build jdk.jfr.javaagent.EventEmitterAgent
 *
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *             jdk.jfr.javaagent.EventEmitterAgent EventEmitterAgent.jar
 *
 * @run main/othervm -Djdk.attach.allowAttachSelf=true jdk.jfr.javaagent.TestLoadedAgent
 */

package jdk.jfr.javaagent;

import com.sun.tools.attach.VirtualMachine;


public class TestLoadedAgent {
    public static void main(String... arg) throws Exception {
        long pid = ProcessHandle.current().pid();
        VirtualMachine vm = VirtualMachine.attach(Long.toString(pid));
        vm.loadAgent("EventEmitterAgent.jar");
        vm.detach();
        EventEmitterAgent.validateRecording();
    }
}
