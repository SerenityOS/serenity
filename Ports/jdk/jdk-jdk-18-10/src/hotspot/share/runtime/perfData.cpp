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
#include "jvm.h"
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/perfData.inline.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"

PerfDataList*   PerfDataManager::_all = NULL;
PerfDataList*   PerfDataManager::_sampled = NULL;
PerfDataList*   PerfDataManager::_constants = NULL;
volatile bool   PerfDataManager::_has_PerfData = 0;

/*
 * The jvmstat global and subsystem jvmstat counter name spaces. The top
 * level name spaces imply the interface stability level of the counter,
 * which generally follows the Java package, class, and property naming
 * conventions. The CounterNS enumeration values should be used to index
 * into this array.
 */
const char* PerfDataManager::_name_spaces[] = {
  // top level name spaces
  "java",                   // stable and supported name space
  "com.sun",                // unstable but supported name space
  "sun",                    // unstable and unsupported name space
  // subsystem name spaces
  "java.gc",                // Garbage Collection name spaces
  "com.sun.gc",
  "sun.gc",
  "java.ci",                // Compiler name spaces
  "com.sun.ci",
  "sun.ci",
  "java.cls",               // Class Loader name spaces
  "com.sun.cls",
  "sun.cls",
  "java.rt",                // Runtime name spaces
  "com.sun.rt",
  "sun.rt",
  "java.os",                // Operating System name spaces
  "com.sun.os",
  "sun.os",
  "java.threads",           // Threads System name spaces
  "com.sun.threads",
  "sun.threads",
  "java.property",          // Java Property name spaces
  "com.sun.property",
  "sun.property",
  "",
};

PerfData::PerfData(CounterNS ns, const char* name, Units u, Variability v)
                  : _name(NULL), _v(v), _u(u), _on_c_heap(false), _valuep(NULL) {

  const char* prefix = PerfDataManager::ns_to_string(ns);

  _name = NEW_C_HEAP_ARRAY(char, strlen(name) + strlen(prefix) + 2, mtInternal);
  assert(strlen(name) != 0, "invalid name");

  if (ns == NULL_NS) {
     // No prefix is added to counters with the NULL_NS namespace.
     strcpy(_name, name);
     // set the F_Supported flag based on the counter name prefix.
     if (PerfDataManager::is_stable_supported(_name) ||
         PerfDataManager::is_unstable_supported(_name)) {
       _flags = F_Supported;
     }
     else {
       _flags = F_None;
     }
  }
  else {
    sprintf(_name, "%s.%s", prefix, name);
    // set the F_Supported flag based on the given namespace.
    if (PerfDataManager::is_stable_supported(ns) ||
        PerfDataManager::is_unstable_supported(ns)) {
      _flags = F_Supported;
    }
    else {
      _flags = F_None;
    }
  }
}

PerfData::~PerfData() {
  FREE_C_HEAP_ARRAY(char, _name);
  if (is_on_c_heap()) {
    FREE_C_HEAP_ARRAY(PerfDataEntry, _pdep);
  }
}

void PerfData::create_entry(BasicType dtype, size_t dsize, size_t vlen) {

  size_t dlen = vlen==0 ? 1 : vlen;

  size_t namelen = strlen(name()) + 1;  // include null terminator
  size_t size = sizeof(PerfDataEntry) + namelen;
  size_t pad_length = ((size % dsize) == 0) ? 0 : dsize - (size % dsize);
  size += pad_length;
  size_t data_start = size;
  size += (dsize * dlen);

  // align size to assure allocation in units of 8 bytes
  int align = sizeof(jlong) - 1;
  size = ((size + align) & ~align);
  char* psmp = PerfMemory::alloc(size);

  if (psmp == NULL) {
    // out of PerfMemory memory resources. allocate on the C heap
    // to avoid vm termination.
    psmp = NEW_C_HEAP_ARRAY(char, size, mtInternal);
    _on_c_heap = true;
  }

  // compute the addresses for the name and data
  char* cname = psmp + sizeof(PerfDataEntry);

  // data is in the last dsize*dlen bytes of the entry
  void* valuep = (void*) (psmp + data_start);

  assert(is_on_c_heap() || PerfMemory::contains(cname), "just checking");
  assert(is_on_c_heap() || PerfMemory::contains((char*)valuep), "just checking");

  // copy the name, including null terminator, into PerfData memory
  strcpy(cname, name());


  // set the header values in PerfData memory
  PerfDataEntry* pdep = (PerfDataEntry*)psmp;
  pdep->entry_length = (jint)size;
  pdep->name_offset = (jint) ((uintptr_t) cname - (uintptr_t) psmp);
  pdep->vector_length = (jint)vlen;
  pdep->data_type = (jbyte) type2char(dtype);
  pdep->data_units = units();
  pdep->data_variability = variability();
  pdep->flags = (jbyte)flags();
  pdep->data_offset = (jint) data_start;

  log_debug(perf, datacreation)("name = %s, dtype = %d, variability = %d,"
                                " units = %d, dsize = " SIZE_FORMAT ", vlen = " SIZE_FORMAT ","
                                " pad_length = " SIZE_FORMAT ", size = " SIZE_FORMAT ", on_c_heap = %s,"
                                " address = " INTPTR_FORMAT ","
                                " data address = " INTPTR_FORMAT,
                                cname, dtype, variability(),
                                units(), dsize, vlen,
                                pad_length, size, is_on_c_heap() ? "TRUE":"FALSE",
                                p2i(psmp), p2i(valuep));

  // record the start of the entry and the location of the data field.
  _pdep = pdep;
  _valuep = valuep;

  // mark the PerfData memory region as having been updated.
  PerfMemory::mark_updated();
}

PerfLong::PerfLong(CounterNS ns, const char* namep, Units u, Variability v)
                 : PerfData(ns, namep, u, v) {

  create_entry(T_LONG, sizeof(jlong));
}

int PerfLong::format(char* buffer, int length) {
  return jio_snprintf(buffer, length, JLONG_FORMAT, *(jlong*)_valuep);
}

PerfLongVariant::PerfLongVariant(CounterNS ns, const char* namep, Units u,
                                 Variability v, jlong* sampled)
                                : PerfLong(ns, namep, u, v),
                                  _sampled(sampled), _sample_helper(NULL) {

  sample();
}

PerfLongVariant::PerfLongVariant(CounterNS ns, const char* namep, Units u,
                                 Variability v, PerfLongSampleHelper* helper)
                                : PerfLong(ns, namep, u, v),
                                  _sampled(NULL), _sample_helper(helper) {

  sample();
}

void PerfLongVariant::sample() {
  if (_sample_helper != NULL) {
    *(jlong*)_valuep = _sample_helper->take_sample();
  }
}

PerfByteArray::PerfByteArray(CounterNS ns, const char* namep, Units u,
                             Variability v, jint length)
                            : PerfData(ns, namep, u, v), _length(length) {

  create_entry(T_BYTE, sizeof(jbyte), (size_t)_length);
}

void PerfString::set_string(const char* s2) {

  // copy n bytes of the string, assuring the null string is
  // copied if s2 == NULL.
  strncpy((char *)_valuep, s2 == NULL ? "" : s2, _length);

  // assure the string is null terminated when strlen(s2) >= _length
  ((char*)_valuep)[_length-1] = '\0';
}

int PerfString::format(char* buffer, int length) {
  return jio_snprintf(buffer, length, "%s", (char*)_valuep);
}

PerfStringConstant::PerfStringConstant(CounterNS ns, const char* namep,
                                       const char* initial_value)
                     : PerfString(ns, namep, V_Constant,
                                  initial_value == NULL ? 1 :
                                  MIN2((jint)(strlen((char*)initial_value)+1),
                                       (jint)(PerfMaxStringConstLength+1)),
                                  initial_value) {

  if (PrintMiscellaneous && Verbose) {
    if (is_valid() && initial_value != NULL &&
        ((jint)strlen(initial_value) > (jint)PerfMaxStringConstLength)) {

      warning("Truncating PerfStringConstant: name = %s,"
              " length = " INT32_FORMAT ","
              " PerfMaxStringConstLength = " INT32_FORMAT "\n",
              namep,
              (jint)strlen(initial_value),
              (jint)PerfMaxStringConstLength);
    }
  }
}


void PerfDataManager::destroy() {

  if (_all == NULL)
    // destroy already called, or initialization never happened
    return;

  // Clear the flag before we free the PerfData counters. Thus begins
  // the race between this thread and another thread that has just
  // queried PerfDataManager::has_PerfData() and gotten back 'true'.
  // The hope is that the other thread will finish its PerfData
  // manipulation before we free the memory. The two alternatives are
  // 1) leak the PerfData memory or 2) do some form of synchronized
  // access or check before every PerfData operation.
  _has_PerfData = false;
  os::naked_short_sleep(1);  // 1ms sleep to let other thread(s) run

  log_debug(perf, datacreation)("Total = %d, Sampled = %d, Constants = %d",
                                _all->length(), _sampled == NULL ? 0 : _sampled->length(),
                                _constants == NULL ? 0 : _constants->length());

  for (int index = 0; index < _all->length(); index++) {
    PerfData* p = _all->at(index);
    delete p;
  }

  delete(_all);
  delete(_sampled);
  delete(_constants);

  _all = NULL;
  _sampled = NULL;
  _constants = NULL;
}

void PerfDataManager::add_item(PerfData* p, bool sampled) {

  MutexLocker ml(PerfDataManager_lock);

  // Default sizes determined using -Xlog:perf+datacreation=debug
  if (_all == NULL) {
    _all = new PerfDataList(191);
    _has_PerfData = true;
  }

  assert(!_all->contains(p->name()), "duplicate name added");

  // add to the list of all perf data items
  _all->append(p);

  if (p->variability() == PerfData::V_Constant) {
    if (_constants == NULL) {
      _constants = new PerfDataList(51);
    }
    _constants->append(p);
    return;
  }

  if (sampled) {
    if (_sampled == NULL) {
      _sampled = new PerfDataList(1);
    }
    _sampled->append(p);
  }
}

PerfDataList* PerfDataManager::all() {

  MutexLocker ml(PerfDataManager_lock);

  if (_all == NULL)
    return NULL;

  PerfDataList* clone = _all->clone();
  return clone;
}

PerfDataList* PerfDataManager::sampled() {

  MutexLocker ml(PerfDataManager_lock);

  if (_sampled == NULL)
    return NULL;

  PerfDataList* clone = _sampled->clone();
  return clone;
}

PerfDataList* PerfDataManager::constants() {

  MutexLocker ml(PerfDataManager_lock);

  if (_constants == NULL)
    return NULL;

  PerfDataList* clone = _constants->clone();
  return clone;
}

char* PerfDataManager::counter_name(const char* ns, const char* name) {
   assert(ns != NULL, "ns string required");
   assert(name != NULL, "name string required");

   size_t len = strlen(ns) + strlen(name) + 2;
   char* result = NEW_RESOURCE_ARRAY(char, len);
   sprintf(result, "%s.%s", ns, name);
   return result;
}

char* PerfDataManager::name_space(const char* ns, const char* sub,
                                  int instance) {
   char intbuf[40];
   jio_snprintf(intbuf, 40, UINT32_FORMAT, instance);
   return name_space(ns, name_space(sub, intbuf));
}

char *PerfDataManager::name_space(const char* ns, int instance) {
   char intbuf[40];
   jio_snprintf(intbuf, 40, UINT32_FORMAT, instance);
   return name_space(ns, intbuf);
}

PerfStringConstant* PerfDataManager::create_string_constant(CounterNS ns,
                                                            const char* name,
                                                            const char* s,
                                                            TRAPS) {

  PerfStringConstant* p = new PerfStringConstant(ns, name, s);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, false);

  return p;
}

PerfLongConstant* PerfDataManager::create_long_constant(CounterNS ns,
                                                        const char* name,
                                                        PerfData::Units u,
                                                        jlong val, TRAPS) {

  PerfLongConstant* p = new PerfLongConstant(ns, name, u, val);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, false);

  return p;
}

PerfStringVariable* PerfDataManager::create_string_variable(CounterNS ns,
                                                            const char* name,
                                                            int max_length,
                                                            const char* s,
                                                            TRAPS) {

  if (max_length == 0 && s != NULL) max_length = (int)strlen(s);

  assert(max_length != 0, "PerfStringVariable with length 0");

  PerfStringVariable* p = new PerfStringVariable(ns, name, max_length, s);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, false);

  return p;
}

PerfLongVariable* PerfDataManager::create_long_variable(CounterNS ns,
                                                        const char* name,
                                                        PerfData::Units u,
                                                        jlong ival, TRAPS) {

  PerfLongVariable* p = new PerfLongVariable(ns, name, u, ival);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, false);

  return p;
}

PerfLongVariable* PerfDataManager::create_long_variable(CounterNS ns,
                                                        const char* name,
                                                        PerfData::Units u,
                                                        jlong* sp, TRAPS) {

  // Sampled counters not supported if UsePerfData is false
  if (!UsePerfData) return NULL;

  PerfLongVariable* p = new PerfLongVariable(ns, name, u, sp);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, true);

  return p;
}

PerfLongVariable* PerfDataManager::create_long_variable(CounterNS ns,
                                                        const char* name,
                                                        PerfData::Units u,
                                                        PerfSampleHelper* sh,
                                                        TRAPS) {

  // Sampled counters not supported if UsePerfData is false
  if (!UsePerfData) return NULL;

  PerfLongVariable* p = new PerfLongVariable(ns, name, u, sh);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, true);

  return p;
}

PerfLongCounter* PerfDataManager::create_long_counter(CounterNS ns,
                                                      const char* name,
                                                      PerfData::Units u,
                                                      jlong ival, TRAPS) {

  PerfLongCounter* p = new PerfLongCounter(ns, name, u, ival);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, false);

  return p;
}

PerfLongCounter* PerfDataManager::create_long_counter(CounterNS ns,
                                                      const char* name,
                                                      PerfData::Units u,
                                                      jlong* sp, TRAPS) {

  // Sampled counters not supported if UsePerfData is false
  if (!UsePerfData) return NULL;

  PerfLongCounter* p = new PerfLongCounter(ns, name, u, sp);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, true);

  return p;
}

PerfLongCounter* PerfDataManager::create_long_counter(CounterNS ns,
                                                      const char* name,
                                                      PerfData::Units u,
                                                      PerfSampleHelper* sh,
                                                      TRAPS) {

  // Sampled counters not supported if UsePerfData is false
  if (!UsePerfData) return NULL;

  PerfLongCounter* p = new PerfLongCounter(ns, name, u, sh);

  if (!p->is_valid()) {
    // allocation of native resources failed.
    delete p;
    THROW_0(vmSymbols::java_lang_OutOfMemoryError());
  }

  add_item(p, true);

  return p;
}

PerfDataList::PerfDataList(int length) {

  _set = new(ResourceObj::C_HEAP, mtInternal) PerfDataArray(length, mtInternal);
}

PerfDataList::PerfDataList(PerfDataList* p) {

  _set = new(ResourceObj::C_HEAP, mtInternal) PerfDataArray(p->length(), mtInternal);

  _set->appendAll(p->get_impl());
}

PerfDataList::~PerfDataList() {

  delete _set;

}

bool PerfDataList::by_name(void* name, PerfData* pd) {

  if (pd == NULL)
    return false;

  return strcmp((const char*)name, pd->name()) == 0;
}

PerfData* PerfDataList::find_by_name(const char* name) {

  int i = _set->find((void*)name, PerfDataList::by_name);

  if (i >= 0 && i <= _set->length())
    return _set->at(i);
  else
    return NULL;
}

PerfDataList* PerfDataList::clone() {

  PerfDataList* copy = new PerfDataList(this);

  assert(copy != NULL, "just checking");

  return copy;
}

PerfTraceTime::~PerfTraceTime() {
  if (!UsePerfData) return;
  _t.stop();
  _timerp->inc(_t.ticks());
}
