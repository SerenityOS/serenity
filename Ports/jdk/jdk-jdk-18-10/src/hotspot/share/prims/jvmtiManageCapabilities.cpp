/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiManageCapabilities.hpp"

static const jint CAPA_SIZE = (JVMTI_INTERNAL_CAPABILITY_COUNT + 7) / 8;

  // capabilities which are always potentially available
jvmtiCapabilities JvmtiManageCapabilities::always_capabilities;

  // capabilities which are potentially available during OnLoad
jvmtiCapabilities JvmtiManageCapabilities::onload_capabilities;

  // capabilities which are always potentially available
  // but to only one environment
jvmtiCapabilities JvmtiManageCapabilities::always_solo_capabilities;

  // capabilities which are potentially available during OnLoad
  // but to only one environment
jvmtiCapabilities JvmtiManageCapabilities::onload_solo_capabilities;

  // remaining capabilities which are always potentially available
  // but to only one environment
jvmtiCapabilities JvmtiManageCapabilities::always_solo_remaining_capabilities;

  // remaining capabilities which are potentially available during OnLoad
  // but to only one environment
jvmtiCapabilities JvmtiManageCapabilities::onload_solo_remaining_capabilities;

  // all capabilities ever acquired
jvmtiCapabilities JvmtiManageCapabilities::acquired_capabilities;

void JvmtiManageCapabilities::initialize() {
  always_capabilities = init_always_capabilities();
  onload_capabilities = init_onload_capabilities();
  always_solo_capabilities = init_always_solo_capabilities();
  onload_solo_capabilities = init_onload_solo_capabilities();
  always_solo_remaining_capabilities = init_always_solo_capabilities();
  onload_solo_remaining_capabilities = init_onload_solo_capabilities();
  memset(&acquired_capabilities, 0, sizeof(acquired_capabilities));
}


// corresponding init functions
jvmtiCapabilities JvmtiManageCapabilities::init_always_capabilities() {
  jvmtiCapabilities jc;

  memset(&jc, 0, sizeof(jc));
  jc.can_get_bytecodes = 1;
  jc.can_signal_thread = 1;
  jc.can_get_source_file_name = 1;
  jc.can_get_line_numbers = 1;
  jc.can_get_synthetic_attribute = 1;
  jc.can_get_monitor_info = 1;
  jc.can_get_constant_pool = 1;
  jc.can_generate_all_class_hook_events = 1;
  jc.can_generate_monitor_events = 1;
  jc.can_generate_garbage_collection_events = 1;
  jc.can_generate_compiled_method_load_events = 1;
  jc.can_generate_native_method_bind_events = 1;
  jc.can_generate_vm_object_alloc_events = 1;
  if (os::is_thread_cpu_time_supported()) {
    jc.can_get_current_thread_cpu_time = 1;
    jc.can_get_thread_cpu_time = 1;
  }
  jc.can_redefine_classes = 1;
  jc.can_redefine_any_class = 1;
  jc.can_retransform_classes = 1;
  jc.can_retransform_any_class = 1;
  jc.can_set_native_method_prefix = 1;
  jc.can_tag_objects = 1;
  jc.can_generate_object_free_events = 1;
  jc.can_generate_resource_exhaustion_heap_events = 1;
  jc.can_generate_resource_exhaustion_threads_events = 1;
  return jc;
}

jvmtiCapabilities JvmtiManageCapabilities::init_onload_capabilities() {
  jvmtiCapabilities jc;

  memset(&jc, 0, sizeof(jc));
#ifndef ZERO
  jc.can_pop_frame = 1;
  jc.can_force_early_return = 1;
  // Workaround for 8195635:
  // disable pop_frame and force_early_return capabilities with Graal
#if INCLUDE_JVMCI
  if (UseJVMCICompiler) {
    jc.can_pop_frame = 0;
    jc.can_force_early_return = 0;
  }
#endif // INCLUDE_JVMCI
#endif // !ZERO
  jc.can_get_source_debug_extension = 1;
  jc.can_access_local_variables = 1;
  jc.can_maintain_original_method_order = 1;
  jc.can_generate_single_step_events = 1;
  jc.can_generate_exception_events = 1;
  jc.can_generate_frame_pop_events = 1;
  jc.can_generate_method_entry_events = 1;
  jc.can_generate_method_exit_events = 1;
  jc.can_get_owned_monitor_info = 1;
  jc.can_get_owned_monitor_stack_depth_info = 1;
  jc.can_get_current_contended_monitor = 1;
  jc.can_generate_early_vmstart = 1;
  jc.can_generate_early_class_hook_events = 1;
  return jc;
}


jvmtiCapabilities JvmtiManageCapabilities::init_always_solo_capabilities() {
  jvmtiCapabilities jc;

  memset(&jc, 0, sizeof(jc));
  jc.can_suspend = 1;
  jc.can_generate_sampled_object_alloc_events = 1;
  return jc;
}


jvmtiCapabilities JvmtiManageCapabilities::init_onload_solo_capabilities() {
  jvmtiCapabilities jc;

  memset(&jc, 0, sizeof(jc));
  jc.can_generate_field_modification_events = 1;
  jc.can_generate_field_access_events = 1;
  jc.can_generate_breakpoint_events = 1;
  return jc;
}


jvmtiCapabilities *JvmtiManageCapabilities::either(const jvmtiCapabilities *a, const jvmtiCapabilities *b,
                                                   jvmtiCapabilities *result) {
  char *ap = (char *)a;
  char *bp = (char *)b;
  char *resultp = (char *)result;

  for (int i = 0; i < CAPA_SIZE; ++i) {
    *resultp++ = *ap++ | *bp++;
  }

  return result;
}


jvmtiCapabilities *JvmtiManageCapabilities::both(const jvmtiCapabilities *a, const jvmtiCapabilities *b,
                                                    jvmtiCapabilities *result) {
  char *ap = (char *)a;
  char *bp = (char *)b;
  char *resultp = (char *)result;

  for (int i = 0; i < CAPA_SIZE; ++i) {
    *resultp++ = *ap++ & *bp++;
  }

  return result;
}


jvmtiCapabilities *JvmtiManageCapabilities::exclude(const jvmtiCapabilities *a, const jvmtiCapabilities *b,
                                                    jvmtiCapabilities *result) {
  char *ap = (char *)a;
  char *bp = (char *)b;
  char *resultp = (char *)result;

  for (int i = 0; i < CAPA_SIZE; ++i) {
    *resultp++ = *ap++ & ~*bp++;
  }

  return result;
}


bool JvmtiManageCapabilities::has_some(const jvmtiCapabilities *a) {
  char *ap = (char *)a;

  for (int i = 0; i < CAPA_SIZE; ++i) {
    if (*ap++ != 0) {
      return true;
    }
  }

  return false;
}


void JvmtiManageCapabilities::copy_capabilities(const jvmtiCapabilities *from, jvmtiCapabilities *to) {
  char *ap = (char *)from;
  char *resultp = (char *)to;

  for (int i = 0; i < CAPA_SIZE; ++i) {
    *resultp++ = *ap++;
  }
}


void JvmtiManageCapabilities::get_potential_capabilities(const jvmtiCapabilities *current,
                                                         const jvmtiCapabilities *prohibited,
                                                         jvmtiCapabilities *result) {
  // exclude prohibited capabilities, must be before adding current
  exclude(&always_capabilities, prohibited, result);

  // must include current since it may possess solo capabilities and now prohibited
  either(result, current, result);

  // add other remaining
  either(result, &always_solo_remaining_capabilities, result);

  // if this is during OnLoad more capabilities are available
  if (JvmtiEnv::get_phase() == JVMTI_PHASE_ONLOAD) {
    either(result, &onload_capabilities, result);
    either(result, &onload_solo_remaining_capabilities, result);
  }
}

jvmtiError JvmtiManageCapabilities::add_capabilities(const jvmtiCapabilities *current,
                                                     const jvmtiCapabilities *prohibited,
                                                     const jvmtiCapabilities *desired,
                                                     jvmtiCapabilities *result) {
  // check that the capabilities being added are potential capabilities
  jvmtiCapabilities temp;
  get_potential_capabilities(current, prohibited, &temp);
  if (has_some(exclude(desired, &temp, &temp))) {
    return JVMTI_ERROR_NOT_AVAILABLE;
  }

  // add to the set of ever acquired capabilities
  either(&acquired_capabilities, desired, &acquired_capabilities);

  // onload capabilities that got added are now permanent - so, also remove from onload
  both(&onload_capabilities, desired, &temp);
  either(&always_capabilities, &temp, &always_capabilities);
  exclude(&onload_capabilities, &temp, &onload_capabilities);

  // same for solo capabilities (transferred capabilities in the remaining sets handled as part of standard grab - below)
  both(&onload_solo_capabilities, desired, &temp);
  either(&always_solo_capabilities, &temp, &always_solo_capabilities);
  exclude(&onload_solo_capabilities, &temp, &onload_solo_capabilities);

  // remove solo capabilities that are now taken
  exclude(&always_solo_remaining_capabilities, desired, &always_solo_remaining_capabilities);
  exclude(&onload_solo_remaining_capabilities, desired, &onload_solo_remaining_capabilities);

  // return the result
  either(current, desired, result);

  update();

  return JVMTI_ERROR_NONE;
}


void JvmtiManageCapabilities::relinquish_capabilities(const jvmtiCapabilities *current,
                                                      const jvmtiCapabilities *unwanted,
                                                      jvmtiCapabilities *result) {
  jvmtiCapabilities to_trash;
  jvmtiCapabilities temp;

  // can't give up what you don't have
  both(current, unwanted, &to_trash);

  // restore solo capabilities but only those that belong
  either(&always_solo_remaining_capabilities, both(&always_solo_capabilities, &to_trash, &temp),
         &always_solo_remaining_capabilities);
  either(&onload_solo_remaining_capabilities, both(&onload_solo_capabilities, &to_trash, &temp),
         &onload_solo_remaining_capabilities);

  update();

  // return the result
  exclude(current, unwanted, result);
}


void JvmtiManageCapabilities::update() {
  jvmtiCapabilities avail;

  // all capabilities
  either(&always_capabilities, &always_solo_capabilities, &avail);

  bool interp_events =
    avail.can_generate_field_access_events ||
    avail.can_generate_field_modification_events ||
    avail.can_generate_single_step_events ||
    avail.can_generate_frame_pop_events ||
    avail.can_generate_method_entry_events ||
    avail.can_generate_method_exit_events;
#ifdef ZERO
  bool enter_all_methods =
    interp_events ||
    avail.can_generate_breakpoint_events;
  if (enter_all_methods) {
    // Disable these when tracking the bytecodes
    UseFastEmptyMethods = false;
    UseFastAccessorMethods = false;
  }
#endif // ZERO

  if (avail.can_generate_breakpoint_events
       || avail.can_generate_field_access_events
       || avail.can_generate_field_modification_events)
  {
    RewriteFrequentPairs = false;
  }

  // If can_redefine_classes is enabled in the onload phase then we know that the
  // dependency information recorded by the compiler is complete.
  if ((avail.can_redefine_classes || avail.can_retransform_classes) &&
      JvmtiEnv::get_phase() == JVMTI_PHASE_ONLOAD) {
    JvmtiExport::set_all_dependencies_are_recorded(true);
  }

  JvmtiExport::set_can_get_source_debug_extension(avail.can_get_source_debug_extension);
  JvmtiExport::set_can_maintain_original_method_order(avail.can_maintain_original_method_order);
  JvmtiExport::set_can_post_interpreter_events(interp_events);
  JvmtiExport::set_can_hotswap_or_post_breakpoint(
    avail.can_generate_breakpoint_events ||
    avail.can_redefine_classes ||
    avail.can_retransform_classes);
  JvmtiExport::set_can_modify_any_class(
    avail.can_generate_breakpoint_events ||
    avail.can_generate_all_class_hook_events);
  JvmtiExport::set_can_walk_any_space(
    avail.can_tag_objects);   // disable sharing in onload phase
  // This controls whether the compilers keep extra locals live to
  // improve the debugging experience so only set them if the selected
  // capabilities look like a debugger.
  JvmtiExport::set_can_access_local_variables(
    avail.can_access_local_variables ||
    avail.can_generate_breakpoint_events ||
    avail.can_generate_frame_pop_events);
  JvmtiExport::set_can_post_on_exceptions(
    avail.can_generate_exception_events ||
    avail.can_generate_frame_pop_events ||
    avail.can_generate_method_exit_events);
  JvmtiExport::set_can_post_breakpoint(avail.can_generate_breakpoint_events);
  JvmtiExport::set_can_post_field_access(avail.can_generate_field_access_events);
  JvmtiExport::set_can_post_field_modification(avail.can_generate_field_modification_events);
  JvmtiExport::set_can_post_method_entry(avail.can_generate_method_entry_events);
  JvmtiExport::set_can_post_method_exit(avail.can_generate_method_exit_events ||
                                        avail.can_generate_frame_pop_events);
  JvmtiExport::set_can_pop_frame(avail.can_pop_frame);
  JvmtiExport::set_can_force_early_return(avail.can_force_early_return);
  JvmtiExport::set_should_clean_up_heap_objects(avail.can_generate_breakpoint_events);
  JvmtiExport::set_can_get_owned_monitor_info(avail.can_get_owned_monitor_info ||
                                              avail.can_get_owned_monitor_stack_depth_info);
}

#ifndef PRODUCT

void JvmtiManageCapabilities:: print(const jvmtiCapabilities* cap) {
  log_trace(jvmti)("----- capabilities -----");
  if (cap->can_tag_objects)
    log_trace(jvmti)("can_tag_objects");
  if (cap->can_generate_field_modification_events)
    log_trace(jvmti)("can_generate_field_modification_events");
  if (cap->can_generate_field_access_events)
    log_trace(jvmti)("can_generate_field_access_events");
  if (cap->can_get_bytecodes)
    log_trace(jvmti)("can_get_bytecodes");
  if (cap->can_get_synthetic_attribute)
    log_trace(jvmti)("can_get_synthetic_attribute");
  if (cap->can_get_owned_monitor_info)
    log_trace(jvmti)("can_get_owned_monitor_info");
  if (cap->can_get_current_contended_monitor)
    log_trace(jvmti)("can_get_current_contended_monitor");
  if (cap->can_get_monitor_info)
    log_trace(jvmti)("can_get_monitor_info");
  if (cap->can_get_constant_pool)
    log_trace(jvmti)("can_get_constant_pool");
  if (cap->can_pop_frame)
    log_trace(jvmti)("can_pop_frame");
  if (cap->can_force_early_return)
    log_trace(jvmti)("can_force_early_return");
  if (cap->can_redefine_classes)
    log_trace(jvmti)("can_redefine_classes");
  if (cap->can_retransform_classes)
    log_trace(jvmti)("can_retransform_classes");
  if (cap->can_signal_thread)
    log_trace(jvmti)("can_signal_thread");
  if (cap->can_get_source_file_name)
    log_trace(jvmti)("can_get_source_file_name");
  if (cap->can_get_line_numbers)
    log_trace(jvmti)("can_get_line_numbers");
  if (cap->can_get_source_debug_extension)
    log_trace(jvmti)("can_get_source_debug_extension");
  if (cap->can_access_local_variables)
    log_trace(jvmti)("can_access_local_variables");
  if (cap->can_maintain_original_method_order)
    log_trace(jvmti)("can_maintain_original_method_order");
  if (cap->can_generate_single_step_events)
    log_trace(jvmti)("can_generate_single_step_events");
  if (cap->can_generate_exception_events)
    log_trace(jvmti)("can_generate_exception_events");
  if (cap->can_generate_frame_pop_events)
    log_trace(jvmti)("can_generate_frame_pop_events");
  if (cap->can_generate_breakpoint_events)
    log_trace(jvmti)("can_generate_breakpoint_events");
  if (cap->can_generate_sampled_object_alloc_events)
    log_trace(jvmti)("can_generate_sampled_object_alloc_events");
  if (cap->can_suspend)
    log_trace(jvmti)("can_suspend");
  if (cap->can_redefine_any_class )
    log_trace(jvmti)("can_redefine_any_class");
  if (cap->can_retransform_any_class )
    log_trace(jvmti)("can_retransform_any_class");
  if (cap->can_get_current_thread_cpu_time)
    log_trace(jvmti)("can_get_current_thread_cpu_time");
  if (cap->can_get_thread_cpu_time)
    log_trace(jvmti)("can_get_thread_cpu_time");
  if (cap->can_generate_method_entry_events)
    log_trace(jvmti)("can_generate_method_entry_events");
  if (cap->can_generate_method_exit_events)
    log_trace(jvmti)("can_generate_method_exit_events");
  if (cap->can_generate_all_class_hook_events)
    log_trace(jvmti)("can_generate_all_class_hook_events");
  if (cap->can_generate_compiled_method_load_events)
    log_trace(jvmti)("can_generate_compiled_method_load_events");
  if (cap->can_generate_monitor_events)
    log_trace(jvmti)("can_generate_monitor_events");
  if (cap->can_generate_vm_object_alloc_events)
    log_trace(jvmti)("can_generate_vm_object_alloc_events");
  if (cap->can_generate_native_method_bind_events)
    log_trace(jvmti)("can_generate_native_method_bind_events");
  if (cap->can_generate_garbage_collection_events)
    log_trace(jvmti)("can_generate_garbage_collection_events");
  if (cap->can_generate_object_free_events)
    log_trace(jvmti)("can_generate_object_free_events");
  if (cap->can_generate_resource_exhaustion_heap_events)
    log_trace(jvmti)("can_generate_resource_exhaustion_heap_events");
  if (cap->can_generate_resource_exhaustion_threads_events)
    log_trace(jvmti)("can_generate_resource_exhaustion_threads_events");
  if (cap->can_generate_early_vmstart)
    log_trace(jvmti)("can_generate_early_vmstart");
  if (cap->can_generate_early_class_hook_events)
    log_trace(jvmti)("can_generate_early_class_hook_events");
}

#endif
