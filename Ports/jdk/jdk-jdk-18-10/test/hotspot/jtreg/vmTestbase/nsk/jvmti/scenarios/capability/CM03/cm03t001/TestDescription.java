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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/capability/CM03/cm03t001.
 * VM Testbase keywords: [jpda, jvmti, onload_only_caps, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test is for CM03 scenario of "capability management".
 *     The test adds all capabilities suitable for debugging at OnLoad phase:
 *         can_get_bytecodes
 *         can_get_synthetic_attribute
 *         can_pop_frame
 *         can_redefine_classes
 *         can_signal_thread
 *         can_get_source_file_name
 *         can_get_line_numbers
 *         can_get_source_debug_extension
 *         can_access_local_variables
 *         can_suspend
 *         can_generate_field_modification_events
 *         can_generate_field_access_events
 *         can_generate_single_step_events
 *         can_generate_exception_events
 *         can_generate_frame_pop_events
 *         can_generate_breakpoint_events
 *         can_generate_method_entry_events
 *         can_generate_method_exit_events
 *     and sets calbacks and enables events correspondent to capabilities above.
 *     Then checks that GetCapabilities returns correct list of possessed
 *     capabilities in Live phase, and checks that correspondent possessed
 *     functions works and requested events are generated.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure cm03t001 is compiled with full debug info
 * @build nsk.jvmti.scenarios.capability.CM03.cm03t001
 * @clean nsk.jvmti.scenarios.capability.CM03.cm03t001
 * @compile -g:lines,source,vars ../cm03t001.java
 *
 * @run main/othervm/native
 *      -agentlib:cm03t001=-waittime=5
 *      nsk.jvmti.scenarios.capability.CM03.cm03t001
 */

