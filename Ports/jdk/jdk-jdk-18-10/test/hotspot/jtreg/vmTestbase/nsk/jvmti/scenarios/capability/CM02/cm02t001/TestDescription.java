/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/scenarios/capability/CM02/cm02t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test is for CM02 scenario of "capability management".
 *     The test adds all capabilities suitable for profiling at OnLoad phase:
 *         can_tag_objects
 *         can_get_owned_monitor_info
 *         can_get_current_contended_monitor
 *         can_get_monitor_info
 *         can_maintain_original_method_order
 *         can_get_current_thread_cpu_time
 *         can_get_thread_cpu_time
 *         can_generate_all_class_hook_events
 *         can_generate_compiled_method_load_events
 *         can_generate_monitor_events
 *         can_generate_vm_object_alloc_events
 *         can_generate_native_method_bind_events
 *         can_generate_garbage_collection_events
 *         can_generate_object_free_events
 *     and sets calbacks and enables events correspondent to capabilities above.
 *     Then checks that GetCapabilities returns correct list of possessed
 *     capabilities in Live phase, and checks that correspondent possessed
 *     functions works and requested events are generated.
 * COMMENTS
 *     Modified due to fix of the bug
 *     5010571 TEST_BUG: jvmti tests with VMObjectAlloc callbacks should
 *             be adjusted to new spec
 *     Modified due to fix of the rfe
 *     5010823 TEST_RFE: some JVMTI tests use the replaced capability
 *     Fixed 5028164 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:cm02t001=-waittime=5
 *      nsk.jvmti.scenarios.capability.CM02.cm02t001
 */

