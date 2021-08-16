/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/os.hpp"
#include "runtime/perfData.inline.hpp"
#include "runtime/statSampler.hpp"
#include "runtime/vm_version.hpp"

// --------------------------------------------------------
// StatSamplerTask

class StatSamplerTask : public PeriodicTask {
  public:
    StatSamplerTask(int interval_time) : PeriodicTask(interval_time) {}
    void task() { StatSampler::collect_sample(); }
};


//----------------------------------------------------------
// Implementation of StatSampler

StatSamplerTask*              StatSampler::_task   = NULL;
PerfDataList*                 StatSampler::_sampled = NULL;

/*
 * the initialize method is called from the engage() method
 * and is responsible for initializing various global variables.
 */
void StatSampler::initialize() {

  if (!UsePerfData) return;

  // create performance data that could not be created prior
  // to vm_init_globals() or otherwise have no logical home.

  create_misc_perfdata();

  // get copy of the sampled list
  _sampled = PerfDataManager::sampled();

}

/*
 * The engage() method is called at initialization time via
 * Thread::create_vm() to initialize the StatSampler and
 * register it with the WatcherThread as a periodic task.
 */
void StatSampler::engage() {

  if (!UsePerfData) return;

  if (!is_active()) {

    initialize();

    // start up the periodic task
    _task = new StatSamplerTask(PerfDataSamplingInterval);
    _task->enroll();
  }
}


/*
 * the disengage() method is responsible for deactivating the periodic
 * task and, if logging was enabled, for logging the final sample. This
 * method is called from before_exit() in java.cpp and is only called
 * after the WatcherThread has been stopped.
 */
void StatSampler::disengage() {

  if (!UsePerfData) return;

  if (!is_active())
    return;

  // remove StatSamplerTask
  _task->disenroll();
  delete _task;
  _task = NULL;

  // force a final sample
  sample_data(_sampled);
}

/*
 * the destroy method is responsible for releasing any resources used by
 * the StatSampler prior to shutdown of the VM. this method is called from
 * before_exit() in java.cpp and is only called after the WatcherThread
 * has stopped.
 */
void StatSampler::destroy() {

  if (!UsePerfData) return;

  if (_sampled != NULL) {
    delete(_sampled);
    _sampled = NULL;
  }
}

/*
 * The sample_data() method is responsible for sampling the
 * the data value for each PerfData instance in the given list.
 */
void StatSampler::sample_data(PerfDataList* list) {

  assert(list != NULL, "null list unexpected");

  for (int index = 0; index < list->length(); index++) {
    PerfData* item = list->at(index);
    item->sample();
  }
}

/*
 * the collect_sample() method is the method invoked by the
 * WatcherThread via the PeriodicTask::task() method. This method
 * is responsible for collecting data samples from sampled
 * PerfData instances every PerfDataSamplingInterval milliseconds.
 * It is also responsible for logging the requested set of
 * PerfData instances every _sample_count milliseconds. While
 * logging data, it will output a column header after every _print_header
 * rows of data have been logged.
 */
void StatSampler::collect_sample() {

  // future - check for new PerfData objects. PerfData objects might
  // get added to the PerfDataManager lists after we have already
  // built our local copies.
  //
  // if (PerfDataManager::count() > previous) {
  //   // get a new copy of the sampled list
  //   if (_sampled != NULL) {
  //     delete(_sampled);
  //     _sampled = NULL;
  //   }
  //   _sampled = PerfDataManager::sampled();
  // }

  assert(_sampled != NULL, "list not initialized");

  sample_data(_sampled);
}

/*
 * Call into java.lang.System.getProperty to check that the value of the
 * specified property matches
 */
void StatSampler::assert_system_property(const char* name, const char* value, TRAPS) {
#ifdef ASSERT
  ResourceMark rm(THREAD);

  // setup the arguments to getProperty
  Handle key_str   = java_lang_String::create_from_str(name, CHECK);

  // return value
  JavaValue result(T_OBJECT);

  // public static String getProperty(String key, String def);
  JavaCalls::call_static(&result,
                         vmClasses::System_klass(),
                         vmSymbols::getProperty_name(),
                         vmSymbols::string_string_signature(),
                         key_str,
                         CHECK);

  oop value_oop = result.get_oop();
  assert(value_oop != NULL, "property must have a value");

  // convert Java String to utf8 string
  char* system_value = java_lang_String::as_utf8_string(value_oop);

  assert(strcmp(value, system_value) == 0, "property value mustn't differ from System.getProperty");
#endif // ASSERT
}

/*
 * Adds a constant counter of the given property. Asserts the value does not
 * differ from the value retrievable from System.getProperty(name)
 */
void StatSampler::add_property_constant(CounterNS name_space, const char* name, const char* value, TRAPS) {
  // the property must exist
  assert(value != NULL, "property name should be have a value: %s", name);
  assert_system_property(name, value, CHECK);
  if (value != NULL) {
    // create the property counter
    PerfDataManager::create_string_constant(name_space, name, value, CHECK);
  }
}

/*
 * Adds a string constant of the given property. Retrieves the value via
 * Arguments::get_property() and asserts the value for the does not differ from
 * the value retrievable from System.getProperty()
 */
void StatSampler::add_property_constant(CounterNS name_space, const char* name, TRAPS) {
  add_property_constant(name_space, name, Arguments::get_property(name), CHECK);
}

/*
 * Method to create PerfStringConstants containing the values of various
 * system properties. Constants are created from information known to HotSpot,
 * but are initialized as-if getting the values from System.getProperty()
 * during bootstrap.
 *
 * Property counters have a counter name space prefix prepended to the
 * property name.
 */
void StatSampler::create_system_property_instrumentation(TRAPS) {

  // Non-writeable, constant properties
  add_property_constant(JAVA_PROPERTY, "java.vm.specification.name", "Java Virtual Machine Specification", CHECK);
  add_property_constant(JAVA_PROPERTY, "java.version", JDK_Version::java_version(), CHECK);
  add_property_constant(JAVA_PROPERTY, "java.vm.version", VM_Version::vm_release(), CHECK);
  add_property_constant(JAVA_PROPERTY, "java.vm.name", VM_Version::vm_name(), CHECK);
  add_property_constant(JAVA_PROPERTY, "java.vm.vendor", VM_Version::vm_vendor(), CHECK);
  add_property_constant(JAVA_PROPERTY, "jdk.debug", VM_Version::jdk_debug_level(), CHECK);

  // Get remaining property constants via Arguments::get_property,
  // which does a linear search over the internal system properties list.

  // SUN_PROPERTY properties
  add_property_constant(SUN_PROPERTY, "sun.boot.library.path", CHECK);

  // JAVA_PROPERTY properties
  add_property_constant(JAVA_PROPERTY, "java.vm.specification.version", CHECK);
  add_property_constant(JAVA_PROPERTY, "java.vm.specification.vendor", CHECK);
  add_property_constant(JAVA_PROPERTY, "java.vm.info", CHECK);
  add_property_constant(JAVA_PROPERTY, "java.library.path", CHECK);
  add_property_constant(JAVA_PROPERTY, "java.class.path", CHECK);
  add_property_constant(JAVA_PROPERTY, "java.home", CHECK);
}

/*
 * The create_misc_perfdata() method provides a place to create
 * PerfData instances that would otherwise have no better place
 * to exist.
 */
void StatSampler::create_misc_perfdata() {

  ResourceMark rm;
  EXCEPTION_MARK;

  // numeric constants

  // frequency of the native high resolution timer
  PerfDataManager::create_constant(SUN_OS, "hrt.frequency",
                                   PerfData::U_Hertz, os::elapsed_frequency(),
                                   CHECK);

  // string constants

  // create string instrumentation for various Java properties.
  create_system_property_instrumentation(CHECK);

  // HotSpot flags (from .hotspotrc) and args (from command line)
  //
  PerfDataManager::create_string_constant(JAVA_RT, "vmFlags",
                                          Arguments::jvm_flags(), CHECK);
  PerfDataManager::create_string_constant(JAVA_RT, "vmArgs",
                                          Arguments::jvm_args(), CHECK);

  // java class name/jar file and arguments to main class
  // note: name is cooridnated with launcher and Arguments.cpp
  PerfDataManager::create_string_constant(SUN_RT, "javaCommand",
                                          Arguments::java_command(), CHECK);

  // the Java VM Internal version string
  PerfDataManager::create_string_constant(SUN_RT, "internalVersion",
                                         VM_Version::internal_vm_info_string(),
                                         CHECK);

  // create sampled instrumentation objects
  create_sampled_perfdata();
}

/*
 * helper class to provide for sampling of the elapsed_counter value
 * maintained in the OS class.
 */
class HighResTimeSampler : public PerfSampleHelper {
  public:
    jlong take_sample() { return os::elapsed_counter(); }
};

/*
 * the create_sampled_perdata() method provides a place to instantiate
 * sampled PerfData instances that would otherwise have no better place
 * to exist.
 */
void StatSampler::create_sampled_perfdata() {

  EXCEPTION_MARK;

  // setup sampling of the elapsed time counter maintained in the
  // the os class. This counter can be used as either a time stamp
  // for each logged entry or as a liveness indicator for the VM.
  PerfSampleHelper* psh = new HighResTimeSampler();
  PerfDataManager::create_counter(SUN_OS, "hrt.ticks",
                                  PerfData::U_Ticks, psh, CHECK);
}
