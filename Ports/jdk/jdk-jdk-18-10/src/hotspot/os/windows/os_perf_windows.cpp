/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "iphlp_interface.hpp"
#include "jvm_io.h"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "pdh_interface.hpp"
#include "runtime/os_perf.hpp"
#include "runtime/os.hpp"
#include "runtime/semaphore.inline.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include CPU_HEADER(vm_version_ext)
#include <math.h>
#include <psapi.h>
#include <TlHelp32.h>

/*
 * Windows provides a vast plethora of performance objects and counters,
 * consumption of which is assisted using the Performance Data Helper (PDH) interface.
 * We import a selected few api entry points from PDH, see pdh_interface.hpp.
 *
 * The code located in this file is to a large extent an abstraction over much of the
 * plumbing needed to start consuming an object and/or counter of choice.
 *
 *
 * How to use:
 * 1. Create a query
 * 2. Add counters to the query
 * 3. Collect the performance data using the query
 * 4. Read the performance data from counters associated with the query
 * 5. Destroy query (counter destruction implied)
 *
 *
 * Every PDH artifact, like processor, process, thread, memory, and so forth are
 * identified with an index that is always the same irrespective
 * of the localized version of the operating system or service pack installed.
 * INFO: Using PDH APIs Correctly in a Localized Language (Q287159)
 *   http://support.microsoft.com/default.aspx?scid=kb;EN-US;q287159
 *
 * To find the correct index for an object or counter, inspect the registry key / value:
 * [HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Perflib\009\Counter]
 *
 * some common PDH indexes
 */
static const DWORD PDH_PROCESSOR_IDX = 238;
static const DWORD PDH_PROCESSOR_TIME_IDX = 6;
static const DWORD PDH_PRIV_PROCESSOR_TIME_IDX = 144;
static const DWORD PDH_PROCESS_IDX = 230;
static const DWORD PDH_ID_PROCESS_IDX = 784;
static const DWORD PDH_CONTEXT_SWITCH_RATE_IDX = 146;
static const DWORD PDH_SYSTEM_IDX = 2;

/* useful pdh fmt's for the general form: \object(instance#index)\counter */
static const char* const OBJECT_COUNTER_FMT = "\\%s\\%s";
static const size_t OBJECT_COUNTER_FMT_LEN = 2;
static const char* const OBJECT_WITH_INSTANCES_COUNTER_FMT = "\\%s(%s)\\%s";
static const size_t OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN = 4;
static const char* const PROCESS_OBJECT_WITH_INSTANCES_COUNTER_FMT = "\\%s(%s#%s)\\%s";
static const size_t PROCESS_OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN = 5;
static const char* const PROCESS_OBJECT_WITH_INSTANCES_WILDCARD_FMT = "\\%s(%s*)\\%s";
static const size_t PROCESS_OBJECT_WITH_INSTANCES_WILDCARD_FMT_LEN = 5;

/* pdh string constants built up from fmts on initialization */
static const char* process_image_name = NULL; // e.g. "java" but could have another image name
static char* pdh_process_instance_IDProcess_counter_fmt = NULL; // "\Process(java#%d)\ID Process" */
static char* pdh_process_instance_wildcard_IDProcess_counter = NULL; // "\Process(java*)\ID Process" */

/*
* Structs for PDH queries.
*/
typedef struct {
  HQUERY pdh_query_handle;
  s8     lastUpdate; // Last time query was updated.
} UpdateQueryS, *UpdateQueryP;


typedef struct {
  UpdateQueryS query;
  HCOUNTER     counter;
  bool         initialized;
} CounterQueryS, *CounterQueryP;

typedef struct {
  UpdateQueryS query;
  HCOUNTER*    counters;
  int          noOfCounters;
  bool         initialized;
} MultiCounterQueryS, *MultiCounterQueryP;

typedef struct {
  MultiCounterQueryP queries;
  int                size;
  bool               initialized;
} MultiCounterQuerySetS, *MultiCounterQuerySetP;

typedef struct {
  MultiCounterQuerySetS set;
  int                   process_idx;
} ProcessQueryS, *ProcessQueryP;

static int open_query(HQUERY* pdh_query_handle) {
  return PdhDll::PdhOpenQuery(NULL, 0, pdh_query_handle) != ERROR_SUCCESS ? OS_ERR : OS_OK;
}

static int open_query(UpdateQueryP query) {
  return open_query(&query->pdh_query_handle);
}

template <typename QueryP>
static int open_query(QueryP query) {
  return open_query(&query->query);
}

static void close_query(HQUERY* const pdh_query_handle, HCOUNTER* const counter) {
  if (counter != NULL && *counter != NULL) {
    PdhDll::PdhRemoveCounter(*counter);
    *counter = NULL;
  }
  if (pdh_query_handle != NULL && *pdh_query_handle != NULL) {
    PdhDll::PdhCloseQuery(*pdh_query_handle);
    *pdh_query_handle = NULL;
  }
}

static void close_query(MultiCounterQueryP query) {
  for (int i = 0; i < query->noOfCounters; ++i) {
    close_query(NULL, &query->counters[i]);
  }
  close_query(&query->query.pdh_query_handle, NULL);
  query->initialized = false;
}

static CounterQueryP create_counter_query() {
  CounterQueryP const query = NEW_C_HEAP_OBJ(CounterQueryS, mtInternal);
  memset(query, 0, sizeof(CounterQueryS));
  return query;
}

static MultiCounterQueryP create_multi_counter_query() {
  MultiCounterQueryP const query = NEW_C_HEAP_ARRAY(MultiCounterQueryS, 1, mtInternal);
  memset(query, 0, sizeof(MultiCounterQueryS));
  return query;
}

static void destroy(CounterQueryP query) {
  assert(query != NULL, "invariant");
  close_query(&query->query.pdh_query_handle, &query->counter);
  FREE_C_HEAP_OBJ(query);
}

static void destroy(MultiCounterQueryP query) {
  if (query != NULL) {
    for (int i = 0; i < query->noOfCounters; ++i) {
      close_query(NULL, &query->counters[i]);
    }
    FREE_C_HEAP_ARRAY(char, query->counters);
    close_query(&query->query.pdh_query_handle, NULL);
    FREE_C_HEAP_ARRAY(MultiCounterQueryS, query);
  }
}

static void destroy_query_set(MultiCounterQuerySetP query_set) {
  for (int i = 0; i < query_set->size; i++) {
    for (int j = 0; j < query_set->queries[i].noOfCounters; ++j) {
      close_query(NULL, &query_set->queries[i].counters[j]);
    }
    FREE_C_HEAP_ARRAY(char, query_set->queries[i].counters);
    close_query(&query_set->queries[i].query.pdh_query_handle, NULL);
  }
  FREE_C_HEAP_ARRAY(MultiCounterQueryS, query_set->queries);
}

static void destroy(MultiCounterQuerySetP query) {
  destroy_query_set(query);
  FREE_C_HEAP_ARRAY(MultiCounterQuerySetS, query);
}

static void destroy(ProcessQueryP query) {
  destroy_query_set(&query->set);
  FREE_C_HEAP_OBJ(query);
}

static void allocate_counters(MultiCounterQueryP query, size_t nofCounters) {
  assert(query != NULL, "invariant");
  assert(!query->initialized, "invariant");
  assert(0 == query->noOfCounters, "invariant");
  assert(query->counters == NULL, "invariant");
  query->counters = NEW_C_HEAP_ARRAY(HCOUNTER, nofCounters, mtInternal);
  memset(query->counters, 0, nofCounters * sizeof(HCOUNTER));
  query->noOfCounters = (int)nofCounters;
}

static void allocate_counters(MultiCounterQuerySetP query, size_t nofCounters) {
  assert(query != NULL, "invariant");
  assert(!query->initialized, "invariant");
  for (int i = 0; i < query->size; ++i) {
    allocate_counters(&query->queries[i], nofCounters);
  }
}

static void allocate_counters(ProcessQueryP query, size_t nofCounters) {
  assert(query != NULL, "invariant");
  allocate_counters(&query->set, nofCounters);
}

static void deallocate_counters(MultiCounterQueryP query) {
  FREE_C_HEAP_ARRAY(char, query->counters);
  query->counters = NULL;
  query->noOfCounters = 0;
}

static OSReturn add_counter(UpdateQueryP query, HCOUNTER* counter, const char* counter_path, bool first_sample_on_init) {
  assert(query != NULL, "invariant");
  assert(counter != NULL, "invariant");
  assert(counter_path != NULL, "invariant");
  if (query->pdh_query_handle == NULL) {
    if (open_query(query) != OS_OK) {
      return OS_ERR;
    }
  }
  assert(query->pdh_query_handle != NULL, "invariant");
  PDH_STATUS status = PdhDll::PdhAddCounter(query->pdh_query_handle, counter_path, 0, counter);
  if (PDH_CSTATUS_NO_OBJECT == status || PDH_CSTATUS_NO_COUNTER == status) {
    return OS_ERR;
  }
  /*
  * According to the MSDN documentation, rate counters must be read twice:
  *
  * "Obtaining the value of rate counters such as Page faults/sec requires that
  *  PdhCollectQueryData be called twice, with a specific time interval between
  *  the two calls, before calling PdhGetFormattedCounterValue. Call Sleep to
  *  implement the waiting period between the two calls to PdhCollectQueryData."
  *
  *  Take the first sample here already to allow for the next "real" sample
  *  to succeed.
  */
  if (first_sample_on_init && PdhDll::PdhCollectQueryData(query->pdh_query_handle) != ERROR_SUCCESS) {
    return OS_ERR;
  }
  return OS_OK;
}

template <typename QueryP>
static OSReturn add_counter(QueryP query, HCOUNTER* counter, const char* counter_path, bool first_sample_on_init) {
  assert(query != NULL, "invariant");
  assert(counter != NULL, "invariant");
  assert(counter_path != NULL, "invariant");
  return add_counter(&query->query, counter, counter_path, first_sample_on_init);
}

// if add_counter fails with OS_ERR, the performance counter might be disabled in the registry
static OSReturn add_counter(CounterQueryP query, const char* counter_path, bool first_sample_on_init = true) {
  return add_counter(query, &query->counter, counter_path, first_sample_on_init);
}

static OSReturn add_counter(MultiCounterQueryP query, int counter_idx, const char* counter_path, bool first_sample_on_init) {
  assert(query != NULL, "invariant");
  assert(counter_idx < query->noOfCounters, "invariant");
  assert(query->counters[counter_idx] == NULL, "invariant");
  return add_counter(query, &query->counters[counter_idx], counter_path, first_sample_on_init);
}

// Need to limit how often we update a query to minimize the heisenberg effect.
// (PDH behaves erratically if the counters are queried too often, especially counters that
// store and use values from two consecutive updates, like cpu load.)
static const int min_update_interval_millis = 500;

static int collect(UpdateQueryP query) {
  assert(query != NULL, "invariant");
  const s8 now = os::javaTimeNanos();
  if (nanos_to_millis(now - query->lastUpdate) > min_update_interval_millis) {
    if (PdhDll::PdhCollectQueryData(query->pdh_query_handle) != ERROR_SUCCESS) {
      return OS_ERR;
    }
    query->lastUpdate = now;
  }
  return OS_OK;
}

template <typename QueryP>
static int collect(QueryP query) {
  assert(query != NULL, "invariant");
  return collect(&query->query);
}

static int formatted_counter_value(HCOUNTER counter, DWORD format, PDH_FMT_COUNTERVALUE* const value) {
  assert(value != NULL, "invariant");
  return PdhDll::PdhGetFormattedCounterValue(counter, format, NULL, value) != ERROR_SUCCESS ? OS_ERR : OS_OK;
}

static int read_counter(CounterQueryP query, DWORD format, PDH_FMT_COUNTERVALUE* const value) {
  assert(query != NULL, "invariant");
  return formatted_counter_value(query->counter, format, value);
}

static int read_counter(MultiCounterQueryP query, int counter_idx, DWORD format, PDH_FMT_COUNTERVALUE* const value) {
  assert(query != NULL, "invariant");
  assert(counter_idx < query->noOfCounters, "invariant");
  assert(query->counters[counter_idx] != NULL, "invariant");
  return formatted_counter_value(query->counters[counter_idx], format, value);
}

static int read_counter(ProcessQueryP query, int counter_idx, DWORD format, PDH_FMT_COUNTERVALUE* const value) {
  assert(query != NULL, "invariant");
  MultiCounterQueryP const current_query = &query->set.queries[query->process_idx];
  assert(current_query != NULL, "invariant");
  return read_counter(current_query, counter_idx, format, value);
}

/*
* The routine expands a process object path including a wildcard to fetch the list of process instances
* having the same name, i.e. "java" or rather the value of process_image_name.
* A tally of this list is returned to the caller.
*/
static int number_of_live_process_instances() {
  char* buffer = NULL;
  DWORD size = 0;
  // determine size
  PDH_STATUS status = PdhDll::PdhExpandWildCardPath(NULL,
                                                    pdh_process_instance_wildcard_IDProcess_counter,
                                                    buffer,
                                                    &size,
                                                    PDH_NOEXPANDCOUNTERS);
  while (status == PDH_MORE_DATA) {
    buffer = NEW_RESOURCE_ARRAY(char, size);
    status = PdhDll::PdhExpandWildCardPath(NULL,
                                           pdh_process_instance_wildcard_IDProcess_counter,
                                           buffer,
                                           &size,
                                           PDH_NOEXPANDCOUNTERS);
  }
  if (status != ERROR_SUCCESS) {
    return OS_ERR;
  }
  // count the number of live process instances
  int instances = 0;
  const char* const end = buffer + size;
  for (char* next = buffer; next != end && (*next != '\0'); next = &next[strlen(next) + 1], ++instances);
  assert(instances > 0, "invariant");
  return instances;
}

static PDH_STATUS pdh_process_idx_to_pid(HQUERY& pdh_query_handle, int idx, LONG* pid) {
  assert(pid != NULL, "invariant");
  char counter_path[PDH_MAX_COUNTER_PATH];
  jio_snprintf(counter_path, sizeof(counter_path) - 1, pdh_process_instance_IDProcess_counter_fmt, idx);
  assert(strlen(counter_path) < sizeof(counter_path), "invariant");
  HCOUNTER counter = NULL;
  PDH_STATUS status = PdhDll::PdhAddCounter(pdh_query_handle, counter_path, 0, &counter);
  if (status != ERROR_SUCCESS) {
    close_query(&pdh_query_handle, &counter);
    return status;
  }
  status = PdhDll::PdhCollectQueryData(pdh_query_handle);
  if (status != ERROR_SUCCESS) {
    close_query(NULL, &counter);
    return PDH_NO_DATA;
  }
  PDH_FMT_COUNTERVALUE counter_value;
  status = formatted_counter_value(counter, PDH_FMT_LONG, &counter_value);
  if (status != OS_OK) {
    close_query(&pdh_query_handle, &counter);
    return status;
  }
  *pid = counter_value.longValue;
  close_query(NULL, &counter);
  return ERROR_SUCCESS;
}


/*
 * Max process query index is derived from the total number of live process instances, seen
 * as a snap-shot at the point of initialization, i.e. processes having the same name, e.g. "java".
 * The total number of live processes includes this process and this number - 1 is the maximum index
 * to be used in a process query.
 */
static int max_process_query_idx = 0;

/*
* Working with the Process object and its related counters is inherently
* problematic when using the PDH API:
*
* A process is not primarily identified by the process id, but by an opaque
* index into a list maintained by the kernel. To distinguish which
* process instance is the intended target for a query, the PDH Process API demands,
* at time of registration, a string describing the target process name concatenated
* with the value for this index. For example:
* "\Process(java#0)", "\Process(java#1)", ...
*
* The bad part is that this list is constantly in-flux as
* processes are exiting. One consequence is that processes with indexes
* greater than the one that just terminated is now shifted down by one.
* For example:
* if \Process(java#1) exits, \Process(java#2) now becomes \Process(java#1),
*    \Process(java#2) becomes \Process(java#1) ...
*
* To make matters even more exciting, an already registered query is not invalidated
* when the process list changes. Instead, the query will continue to work just as before,
* or at least, so it seems.
* Only, now, the query will read performance data from another process instance!
* That's right, the performance data is now read from the process that was shifted
* down by the kernel to occupy the index slot associated with our original registration.
*
* Solution:
* The #index identifier for a Process query can only decrease after process creation.
*
* We therefore create an array of counter queries for all process object instances
* up to and including ourselves:
*
* E.g. we come in as the third process instance (java#2), we then create and register
* queries for the following Process object instances:
* java#0, java#1, java#2
*
* current_process_query_index() finds the "correct" pdh process query index by inspecting
* the pdh process list, at a particular instant, i.e. just before we issue the real process query.
* Of course, this is an inherently racy situation because the pdh process list can change at any time.
* We use current_process_query_index() to help keep the number of data errors low,
* where a data error is defined to be the result of using a stale index to query the wrong process.
*
* Ensure to call ensure_current_process_query_index() before every query involving Process object instance data.
*
* returns OS_ERR(-1) if anything goes wrong in the discovery process.
*/

static int current_process_query_index(int previous_query_idx = 0) {
  assert(max_process_query_idx >= 0, "invariant");
  assert(max_process_query_idx >= previous_query_idx, "invariant");
  assert(process_image_name != NULL, "invariant");
  assert(pdh_process_instance_IDProcess_counter_fmt != NULL, "invariant");
  int result = OS_ERR;
  HQUERY tmp_pdh_query_handle = NULL;
  if (open_query(&tmp_pdh_query_handle) != OS_OK) {
    return OS_ERR;
  }
  // We need to find the correct pdh process index corresponding to our process identifier (pid).
  // Begin from the index that was valid at the time of the last query. If that index is no longer valid,
  // it means the pdh process list has changed, i.e. because other processes with the same name as us have terminated.
  // Seek downwards to find the updated, now downshifted, list index corresponding to our pid.
  static const LONG current_pid = (LONG)os::current_process_id();
  const int start_idx = previous_query_idx != 0 ? previous_query_idx : max_process_query_idx;
  for (int idx = start_idx; idx >= 0; --idx) {
    LONG pid;
    const PDH_STATUS status = pdh_process_idx_to_pid(tmp_pdh_query_handle, idx, &pid);
    if (status == PDH_NO_DATA) {
      // pdh process list has changed
      continue;
    }
    if (status != ERROR_SUCCESS) {
      // something went wrong, tmp_pdh_query_handle is already closed.
      return OS_ERR;
    }
    if (current_pid == pid) {
      result = idx;
      break;
    }
  }
  close_query(&tmp_pdh_query_handle, NULL);
  return result;
}

static int ensure_current_process_query_index(ProcessQueryP query) {
  assert(query != NULL, "invariant");
  const int previous_query_idx = query->process_idx;
  if (previous_query_idx == 0) {
    return previous_query_idx;
  }
  const int current_query_idx = current_process_query_index(previous_query_idx);
  if (current_query_idx == OS_ERR || current_query_idx >= query->set.size) {
    return OS_ERR;
  }
  if (current_query_idx == previous_query_idx) {
    return previous_query_idx;
  }
  assert(current_query_idx >= 0 && current_query_idx < query->set.size, "out of bounds!");
  while (current_query_idx < query->set.size - 1) {
    const int new_size = --query->set.size;
    close_query(&query->set.queries[new_size]);
  }
  assert(current_query_idx < query->set.size, "invariant");
  query->process_idx = current_query_idx;
  return OS_OK;
}

static MultiCounterQueryP current_process_query(ProcessQueryP query) {
  assert(query != NULL, "invariant");
  if (ensure_current_process_query_index(query) == OS_ERR) {
    return NULL;
  }
  assert(query->process_idx < query->set.size, "invariant");
  return &query->set.queries[query->process_idx];
}

static int collect(ProcessQueryP query) {
  assert(query != NULL, "invariant");
  MultiCounterQueryP current_query = current_process_query(query);
  return current_query != NULL ? collect(current_query) : OS_ERR;
}

/*
 * Construct a fully qualified PDH counter path.
 *
 * @param object_name   a PDH Object string representation(required)
 * @param counter_name  a PDH Counter string representation(required)
 * @param image_name    a process image name string, ex. "java" (opt)
 * @param instance      an instance string, ex. "0", "1", ... (opt)
 * @return              the fully qualified PDH counter path.
 *
 * Caller will need a ResourceMark.
 *
 * (PdhMakeCounterPath() seems buggy on concatenating instances, hence this function instead)
 */
static const char* make_fully_qualified_counter_path(const char* object_name,
                                                     const char* counter_name,
                                                     const char* image_name = NULL,
                                                     const char* instance = NULL) {
  assert(object_name != NULL, "invariant");
  assert(counter_name != NULL, "invariant");
  size_t counter_path_len = strlen(object_name) + strlen(counter_name);

  char* counter_path;
  size_t jio_snprintf_result = 0;
  if (image_name) {
    /*
    * For paths using the "Process" Object.
    *
    * Examples:
    * form:   "\object_name(image_name#instance)\counter_name"
    * actual: "\Process(java#2)\ID Process"
    */
    counter_path_len += PROCESS_OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN;
    counter_path_len += strlen(image_name);
    /*
    * image_name must be passed together with an associated
    * instance "number" ("0", "1", "2", ...).
    * This is required in order to create valid "Process" Object paths.
    *
    * Examples: "\Process(java#0)", \Process(java#1"), ...
    */
    assert(instance != NULL, "invariant");
    counter_path_len += strlen(instance);
    counter_path = NEW_RESOURCE_ARRAY(char, counter_path_len + 1);
    jio_snprintf_result = jio_snprintf(counter_path,
                                       counter_path_len + 1,
                                       PROCESS_OBJECT_WITH_INSTANCES_COUNTER_FMT,
                                       object_name,
                                       image_name,
                                       instance,
                                       counter_name);
  } else {
    if (instance) {
      /*
      * For paths where the Object has multiple instances.
      *
      * Examples:
      * form:   "\object_name(instance)\counter_name"
      * actual: "\Processor(0)\% Privileged Time"
      */
      counter_path_len += strlen(instance);
      counter_path_len += OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN;
    } else {
      /*
      * For "normal" paths.
      *
      * Examples:
      * form:   "\object_name\counter_name"
      * actual: "\Memory\Available Mbytes"
      */
      counter_path_len += OBJECT_COUNTER_FMT_LEN;
    }
    counter_path = NEW_RESOURCE_ARRAY(char, counter_path_len + 1);
    if (instance) {
      jio_snprintf_result = jio_snprintf(counter_path,
                                         counter_path_len + 1,
                                         OBJECT_WITH_INSTANCES_COUNTER_FMT,
                                         object_name,
                                         instance,
                                         counter_name);
    } else {
      jio_snprintf_result = jio_snprintf(counter_path,
                                         counter_path_len + 1,
                                         OBJECT_COUNTER_FMT,
                                         object_name,
                                         counter_name);
    }
  }
  assert(counter_path_len == jio_snprintf_result, "invariant");
  return counter_path;
}

static void log_invalid_pdh_index(DWORD index) {
  log_warning(os)("Unable to resolve PDH index: (%ld)", index);
  log_warning(os)("Please check the registry if this performance object/counter is disabled");
}

static bool is_valid_pdh_index(DWORD index) {
  DWORD dummy = 0;
  if (PdhDll::PdhLookupPerfNameByIndex(NULL, index, NULL, &dummy) != PDH_MORE_DATA) {
    log_invalid_pdh_index(index);
    return false;
  }
  return true;
}

/*
 * Maps an index to a resource area allocated string for the localized PDH artifact.
 *
 * Caller will need a ResourceMark.
 *
 * @param index    the counter index as specified in the registry
 * @param p_string pointer to a char*
 * @return         OS_OK if successful, OS_ERR on failure.
 */
static OSReturn lookup_name_by_index(DWORD index, char** p_string) {
  assert(p_string != NULL, "invariant");
  if (!is_valid_pdh_index(index)) {
    return OS_ERR;
  }
  // determine size needed
  DWORD size = 0;
  PDH_STATUS status = PdhDll::PdhLookupPerfNameByIndex(NULL, index, NULL, &size);
  assert(status == PDH_MORE_DATA, "invariant");
  *p_string = NEW_RESOURCE_ARRAY(char, size);
  if (PdhDll::PdhLookupPerfNameByIndex(NULL, index, *p_string, &size) != ERROR_SUCCESS) {
    return OS_ERR;
  }
  if (0 == size || *p_string == NULL) {
    return OS_ERR;
  }
  // windows vista does not null-terminate the string (although the docs says it will)
  (*p_string)[size - 1] = '\0';
  return OS_OK;
}

static const char* copy_string_to_c_heap(const char* string) {
  assert(string != NULL, "invariant");
  const size_t len = strlen(string);
  char* const cheap_allocated_string = NEW_C_HEAP_ARRAY(char, len + 1, mtInternal);
  strncpy(cheap_allocated_string, string, len + 1);
  return cheap_allocated_string;
}

/*
* Maps a pdh artifact index to a resource area allocated string representing a localized name.
*
* Caller will need a ResourceMark.
*
* @param pdh_artifact_idx   the counter index as specified in the registry
* @return                   localized pdh artifact string if successful, NULL on failure.
*/
static const char* pdh_localized_artifact(DWORD pdh_artifact_idx) {
  char* pdh_localized_artifact_string = NULL;
  // get localized name for the pdh artifact idx
  if (lookup_name_by_index(pdh_artifact_idx, &pdh_localized_artifact_string) != OS_OK) {
    return NULL;
  }
  return pdh_localized_artifact_string;
}

/*
 * Returns the PDH string identifying the current process image name.
 * Use this prefix when getting counters from the PDH process object
 * representing your process.
 * Ex. "Process(java#0)\Virtual Bytes" - where "java" is the PDH process
 * image description.
 *
 * Caller needs ResourceMark.
 *
 * @return the process image string description, NULL if the call failed.
*/
static const char* pdh_process_image_name() {
  char* module_name = NEW_RESOURCE_ARRAY(char, MAX_PATH);
  // Find our module name and use it to extract the image name used by PDH
  DWORD getmfn_return = GetModuleFileName(NULL, module_name, MAX_PATH);
  if (getmfn_return >= MAX_PATH || 0 == getmfn_return) {
    return NULL;
  }
  if (os::get_last_error() == ERROR_INSUFFICIENT_BUFFER) {
    return NULL;
  }
  char* process_image_name = strrchr(module_name, '\\'); //drop path
  process_image_name++;                                  //skip slash
  char* dot_pos = strrchr(process_image_name, '.');      //drop .exe
  dot_pos[0] = '\0';
  return process_image_name;
}

static void deallocate_pdh_constants() {
  FREE_C_HEAP_ARRAY(char, process_image_name);
  process_image_name = NULL;
  FREE_C_HEAP_ARRAY(char, pdh_process_instance_IDProcess_counter_fmt);
  pdh_process_instance_IDProcess_counter_fmt = NULL;
  FREE_C_HEAP_ARRAY(char, pdh_process_instance_wildcard_IDProcess_counter);
  pdh_process_instance_wildcard_IDProcess_counter = NULL;
}

static OSReturn allocate_pdh_constants() {
  assert(process_image_name == NULL, "invariant");
  const char* pdh_image_name = pdh_process_image_name();
  if (pdh_image_name == NULL) {
    return OS_ERR;
  }
  process_image_name = copy_string_to_c_heap(pdh_image_name);

  const char* pdh_localized_process_object = pdh_localized_artifact(PDH_PROCESS_IDX);
  if (pdh_localized_process_object == NULL) {
    return OS_ERR;
  }

  const char* pdh_localized_IDProcess_counter = pdh_localized_artifact(PDH_ID_PROCESS_IDX);
  if (pdh_localized_IDProcess_counter == NULL) {
    return OS_ERR;
  }

  const size_t id_process_base_length = strlen(process_image_name) +
                                        strlen(pdh_localized_process_object) +
                                        strlen(pdh_localized_IDProcess_counter);

  const size_t pdh_IDProcess_counter_fmt_len = id_process_base_length +
                                               PROCESS_OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN +
                                               2; // "%d"

  assert(pdh_process_instance_IDProcess_counter_fmt == NULL, "invariant");
  pdh_process_instance_IDProcess_counter_fmt = NEW_C_HEAP_ARRAY(char, pdh_IDProcess_counter_fmt_len + 1, mtInternal);

  /* "\Process(java#%d)\ID Process" */
  size_t len = jio_snprintf(pdh_process_instance_IDProcess_counter_fmt,
                            pdh_IDProcess_counter_fmt_len + 1,
                            PROCESS_OBJECT_WITH_INSTANCES_COUNTER_FMT,
                            pdh_localized_process_object,
                            process_image_name,
                            "%d",
                            pdh_localized_IDProcess_counter);

  assert(pdh_process_instance_IDProcess_counter_fmt != NULL, "invariant");
  assert(len == pdh_IDProcess_counter_fmt_len, "invariant");


  const size_t pdh_IDProcess_wildcard_fmt_len = id_process_base_length +
                                                PROCESS_OBJECT_WITH_INSTANCES_WILDCARD_FMT_LEN;

  assert(pdh_process_instance_wildcard_IDProcess_counter == NULL, "invariant");
  pdh_process_instance_wildcard_IDProcess_counter = NEW_C_HEAP_ARRAY(char, pdh_IDProcess_wildcard_fmt_len + 1, mtInternal);

  /* "\Process(java*)\ID Process" */
  len = jio_snprintf(pdh_process_instance_wildcard_IDProcess_counter,
                     pdh_IDProcess_wildcard_fmt_len + 1,
                     PROCESS_OBJECT_WITH_INSTANCES_WILDCARD_FMT,
                     pdh_localized_process_object,
                     process_image_name,
                     pdh_localized_IDProcess_counter);

  assert(pdh_process_instance_wildcard_IDProcess_counter != NULL, "invariant");
  assert(len == pdh_IDProcess_wildcard_fmt_len, "invariant");
  return OS_OK;
}

/*
 * Enuerate the Processor PDH object and returns a buffer containing the enumerated instances.
 * Caller needs ResourceMark;
 *
 * @return  buffer if successful, NULL on failure.
*/
static const char* enumerate_cpu_instances() {
  char* processor; //'Processor' == PDH_PROCESSOR_IDX
  if (lookup_name_by_index(PDH_PROCESSOR_IDX, &processor) != OS_OK) {
    return NULL;
  }
  DWORD c_size = 0;
  DWORD i_size = 0;
  // enumerate all processors.
  PDH_STATUS pdhStat = PdhDll::PdhEnumObjectItems(NULL, // reserved
                                                  NULL, // local machine
                                                  processor, // object to enumerate
                                                  NULL,
                                                  &c_size,
                                                  NULL, // instance buffer is NULL and
                                                  &i_size,  // pass 0 length in order to get the required size
                                                  PERF_DETAIL_WIZARD, // counter detail level
                                                  0);
  if (PdhDll::PdhStatusFail((pdhStat))) {
    return NULL;
  }
  char* const instances = NEW_RESOURCE_ARRAY(char, i_size);
  c_size = 0;
  pdhStat = PdhDll::PdhEnumObjectItems(NULL, // reserved
                                       NULL, // local machine
                                       processor, // object to enumerate
                                       NULL,
                                       &c_size,
                                       instances, // now instance buffer is allocated to be filled in
                                       &i_size, // and the required size is known
                                       PERF_DETAIL_WIZARD, // counter detail level
                                       0);
  return PdhDll::PdhStatusFail(pdhStat) ? NULL : instances;
}

static int count_logical_cpus(const char* instances) {
  assert(instances != NULL, "invariant");
  // count logical instances.
  DWORD count;
  char* tmp;
  for (count = 0, tmp = const_cast<char*>(instances); *tmp != '\0'; tmp = &tmp[strlen(tmp) + 1], count++);
  // PDH reports an instance for each logical processor plus an instance for the total (_Total)
  assert(count == os::processor_count() + 1, "invalid enumeration!");
  return count - 1;
}

static int number_of_logical_cpus() {
  static int numberOfCPUS = 0;
  if (numberOfCPUS == 0) {
    const char* instances = enumerate_cpu_instances();
    if (instances == NULL) {
      return OS_ERR;
    }
    numberOfCPUS = count_logical_cpus(instances);
  }
  return numberOfCPUS;
}

static double cpu_factor() {
  static DWORD numCpus = 0;
  static double cpuFactor = .0;
  if (numCpus == 0) {
    numCpus = number_of_logical_cpus();
    assert(os::processor_count() <= (int)numCpus, "invariant");
    cpuFactor = numCpus * 100;
  }
  return cpuFactor;
}

static void log_error_message_on_no_PDH_artifact(const char* counter_path) {
  log_warning(os)("Unable to register PDH query for \"%s\"", counter_path);
  log_warning(os)("Please check the registry if this performance object/counter is disabled");
}

static int initialize_cpu_query_counters(MultiCounterQueryP query, DWORD pdh_counter_idx) {
  assert(query != NULL, "invariant");
  assert(query->counters != NULL, "invariant");
  char* processor; //'Processor' == PDH_PROCESSOR_IDX
  if (lookup_name_by_index(PDH_PROCESSOR_IDX, &processor) != OS_OK) {
    return OS_ERR;
  }
  char* counter_name = NULL;
  if (lookup_name_by_index(pdh_counter_idx, &counter_name) != OS_OK) {
    return OS_ERR;
  }
  if (query->query.pdh_query_handle == NULL) {
    if (open_query(query) != OS_OK) {
      return OS_ERR;
    }
  }
  assert(query->query.pdh_query_handle != NULL, "invariant");
  size_t counter_len = strlen(processor);
  counter_len += strlen(counter_name);
  counter_len += OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN; // "\\%s(%s)\\%s"
  const char* instances = enumerate_cpu_instances();
  DWORD index = 0;
  for (char* tmp = const_cast<char*>(instances); *tmp != '\0'; tmp = &tmp[strlen(tmp) + 1], index++) {
    const size_t tmp_len = strlen(tmp);
    char* counter_path = NEW_RESOURCE_ARRAY(char, counter_len + tmp_len + 1);
    const size_t jio_snprintf_result = jio_snprintf(counter_path,
                                                    counter_len + tmp_len + 1,
                                                    OBJECT_WITH_INSTANCES_COUNTER_FMT,
                                                    processor,
                                                    tmp, // instance "0", "1", .."_Total"
                                                    counter_name);
    assert(counter_len + tmp_len == jio_snprintf_result, "invariant");
    if (add_counter(query, &query->counters[index], counter_path, false) != OS_OK) {
      // performance counter is disabled in registry and not accessible via PerfLib
      log_error_message_on_no_PDH_artifact(counter_path);
      // return OS_OK to have the system continue to run without the missing counter
      return OS_OK;
    }
  }
  // Query once to initialize the counters which require at least two samples
  // (like the % CPU usage) to calculate correctly.
  return PdhDll::PdhCollectQueryData(query->query.pdh_query_handle) != ERROR_SUCCESS ? OS_ERR : OS_OK;
}

static int initialize_cpu_query(MultiCounterQueryP query) {
  assert(query != NULL, "invariant");
  assert(!query->initialized, "invariant");
  const int logical_cpu_count = number_of_logical_cpus();
  assert(logical_cpu_count >= os::processor_count(), "invariant");
  // we also add another counter for instance "_Total"
  allocate_counters(query, logical_cpu_count + 1);
  assert(query->noOfCounters == logical_cpu_count + 1, "invariant");
  if (initialize_cpu_query_counters(query, PDH_PROCESSOR_TIME_IDX) != OS_OK) {
    return OS_ERR;
  }
  query->initialized = true;
  return OS_OK;
}

static int initialize_query(CounterQueryP query, DWORD pdh_object_idx, DWORD pdh_counter_idx) {
  assert(query != NULL, "invariant");
  assert(!query->initialized, "invariant");
  if (!((is_valid_pdh_index(pdh_object_idx) && is_valid_pdh_index(pdh_counter_idx)))) {
    return OS_ERR;
  }
  const char* object = pdh_localized_artifact(pdh_object_idx);
  assert(object != NULL, "invariant");
  const char* counter = pdh_localized_artifact(pdh_counter_idx);
  assert(counter != NULL, "invariant");
  const char* counter_path = make_fully_qualified_counter_path(object, counter);
  assert(counter_path != NULL, "invariant");
  if (add_counter(query, counter_path, true) != OS_OK) {
    return OS_ERR;
  }
  query->initialized = true;
  return OS_OK;
}

static int initialize_context_switches_query(CounterQueryP query) {
  return initialize_query(query, PDH_SYSTEM_IDX, PDH_CONTEXT_SWITCH_RATE_IDX);
}

static ProcessQueryP create_process_query() {
  const int current_process_query_idx = current_process_query_index();
  if (current_process_query_idx == OS_ERR) {
    return NULL;
  }
  ProcessQueryP const query = NEW_C_HEAP_OBJ(ProcessQueryS, mtInternal);
  memset(query, 0, sizeof(ProcessQueryS));
  query->process_idx = current_process_query_idx;
  const int size = current_process_query_idx + 1;
  query->set.queries = NEW_C_HEAP_ARRAY(MultiCounterQueryS, size, mtInternal);
  memset(query->set.queries, 0, sizeof(MultiCounterQueryS) * size);
  query->set.size = size;
  return query;
}

static int initialize_process_counter(ProcessQueryP process_query, int counter_idx, DWORD pdh_counter_idx) {
  char* localized_process_object;
  if (lookup_name_by_index(PDH_PROCESS_IDX, &localized_process_object) != OS_OK) {
    return OS_ERR;
  }
  assert(localized_process_object != NULL, "invariant");
  char* localized_counter_name;
  if (lookup_name_by_index(pdh_counter_idx, &localized_counter_name) != OS_OK) {
    return OS_ERR;
  }
  assert(localized_counter_name != NULL, "invariant");
  for (int i = 0; i < process_query->set.size; ++i) {
    char instanceIndexBuffer[32];
    const char* counter_path = make_fully_qualified_counter_path(localized_process_object,
                                                                 localized_counter_name,
                                                                 process_image_name,
                                                                 itoa(i, instanceIndexBuffer, 10));
    assert(counter_path != NULL, "invariant");
    MultiCounterQueryP const query = &process_query->set.queries[i];
    if (add_counter(query, counter_idx, counter_path, true) != OS_OK) {
      return OS_ERR;
    }
    if (counter_idx + 1 == query->noOfCounters) {
      // last counter in query implies query initialized
      query->initialized = true;
    }
  }
  return OS_OK;
}

static int initialize_process_query(ProcessQueryP query) {
  assert(query != NULL, "invariant");
  assert(!query->set.initialized, "invariant");
  allocate_counters(query, 2);
  if (initialize_process_counter(query, 0, PDH_PROCESSOR_TIME_IDX) != OS_OK) {
    return OS_ERR;
  }
  if (initialize_process_counter(query, 1, PDH_PRIV_PROCESSOR_TIME_IDX) != OS_OK) {
    return OS_ERR;
  }
  query->set.initialized = true;
  return OS_OK;
}

static int reference_count = 0;
static bool pdh_initialized = false;

class PdhMutex : public StackObj {
 private:
  static Semaphore _semaphore;
 public:
  PdhMutex() {
    _semaphore.wait();
  }
  ~PdhMutex() {
    _semaphore.signal();
  }
};

Semaphore PdhMutex::_semaphore(1);

static void on_initialization_failure() {
  // still holder of mutex
  assert(max_process_query_idx == 0, "invariant");
  deallocate_pdh_constants();
  --reference_count;
  PdhDll::PdhDetach();
}

static OSReturn initialize() {
  // still holder of mutex
  ResourceMark rm;
  if (!PdhDll::PdhAttach()) {
    return OS_ERR;
  }
  if (allocate_pdh_constants() != OS_OK) {
    on_initialization_failure();
    return OS_ERR;
  }
  // Take a snapshot of the current number of live processes (including ourselves)
  // with the same name, e.g. "java", in order to derive a value for max_process_query_idx.
  const int process_instance_count = number_of_live_process_instances();
  if (process_instance_count == OS_ERR) {
    on_initialization_failure();
    return OS_ERR;
  }
  assert(process_instance_count > 0, "invariant");
  max_process_query_idx = process_instance_count - 1;
  return OS_OK;
}

/*
* Helper to initialize the PDH library, function pointers, constants and counters.
*
* Reference counting allows for unloading of pdh.dll granted all sessions use the pair:
*
*   pdh_acquire();
*   pdh_release();
*
* @return  OS_OK if successful, OS_ERR on failure.
*/
static OSReturn pdh_acquire() {
  PdhMutex mutex;
  reference_count++;
  if (pdh_initialized) {
    return OS_OK;
  }
  const OSReturn status = initialize();
  pdh_initialized = status == OS_OK;
  return status;
}

static void pdh_release() {
  PdhMutex mutex;
  if (1 == reference_count--) {
    deallocate_pdh_constants();
    PdhDll::PdhDetach();
    pdh_initialized = false;
  }
}

class CPUPerformanceInterface::CPUPerformance : public CHeapObj<mtInternal> {
  friend class CPUPerformanceInterface;
 private:
  CounterQueryP _context_switches;
  ProcessQueryP _process_cpu_load;
  MultiCounterQueryP _machine_cpu_load;

  int cpu_load(int which_logical_cpu, double* cpu_load);
  int context_switch_rate(double* rate);
  int cpu_load_total_process(double* cpu_load);
  int cpu_loads_process(double* jvm_user_load, double* jvm_kernel_load, double* system_total_load);
  CPUPerformance();
  ~CPUPerformance();
  bool initialize();
};

CPUPerformanceInterface::CPUPerformance::CPUPerformance() : _context_switches(NULL), _process_cpu_load(NULL), _machine_cpu_load(NULL) {}

bool CPUPerformanceInterface::CPUPerformance::initialize() {
  if (pdh_acquire() != OS_OK) {
    return false;
  }
  _context_switches = create_counter_query();
  assert(_context_switches != NULL, "invariant");
  if (initialize_context_switches_query(_context_switches) != OS_OK) {
    return false;
  }
  assert(_context_switches->initialized, "invariant");
  _process_cpu_load = create_process_query();
  if (_process_cpu_load == NULL) {
    return false;
  }
  if (initialize_process_query(_process_cpu_load) != OS_OK) {
    return false;
  }
  assert(_process_cpu_load->set.initialized, "invariant");
  _machine_cpu_load = create_multi_counter_query();
  assert(_machine_cpu_load != NULL, "invariant");
  if (initialize_cpu_query(_machine_cpu_load) != OS_OK) {
    return false;
  }
  assert(_machine_cpu_load->initialized, "invariant");
  return true;
}

CPUPerformanceInterface::CPUPerformance::~CPUPerformance() {
  if (_context_switches != NULL) {
    destroy(_context_switches);
    _context_switches = NULL;
  }
  if (_process_cpu_load != NULL) {
    destroy(_process_cpu_load);
    _process_cpu_load = NULL;
  }
  if (_machine_cpu_load != NULL) {
    destroy(_machine_cpu_load);
    _machine_cpu_load = NULL;
  }
  pdh_release();
}

CPUPerformanceInterface::CPUPerformanceInterface() : _impl(NULL) {}

bool CPUPerformanceInterface::initialize() {
  _impl = new CPUPerformanceInterface::CPUPerformance();
  return _impl->initialize();
}

CPUPerformanceInterface::~CPUPerformanceInterface() {
  if (_impl != NULL) {
    delete _impl;
  }
}

int CPUPerformanceInterface::cpu_load(int which_logical_cpu, double* cpu_load) const {
  return _impl->cpu_load(which_logical_cpu, cpu_load);
}

int CPUPerformanceInterface::context_switch_rate(double* rate) const {
  return _impl->context_switch_rate(rate);
}

int CPUPerformanceInterface::cpu_load_total_process(double* cpu_load) const {
  return _impl->cpu_load_total_process(cpu_load);
}

int CPUPerformanceInterface::cpu_loads_process(double* jvm_user_load,
                                               double* jvm_kernel_load,
                                               double* system_total_load) const {
  return _impl->cpu_loads_process(jvm_user_load, jvm_kernel_load, system_total_load);
}

int CPUPerformanceInterface::CPUPerformance::cpu_load(int which_logical_cpu, double* cpu_load) {
  *cpu_load = .0;
  if (_machine_cpu_load == NULL || !_machine_cpu_load->initialized) {
    return OS_ERR;
  }
  assert(which_logical_cpu < _machine_cpu_load->noOfCounters, "invariant");
  if (collect(_machine_cpu_load) != OS_OK) {
    return OS_ERR;
  }
  // -1 is total (all cpus)
  const int counter_idx = -1 == which_logical_cpu ? _machine_cpu_load->noOfCounters - 1 : which_logical_cpu;
  PDH_FMT_COUNTERVALUE counter_value;
  if (read_counter(_machine_cpu_load, counter_idx, PDH_FMT_DOUBLE, &counter_value) != OS_OK) {
    return OS_ERR;
  }
  *cpu_load = counter_value.doubleValue / 100;
  return OS_OK;
}

int CPUPerformanceInterface::CPUPerformance::cpu_load_total_process(double* cpu_load) {
  *cpu_load = .0;
  if (_process_cpu_load == NULL || !_process_cpu_load->set.initialized) {
    return OS_ERR;
  }
  if (collect(_process_cpu_load) != OS_OK) {
    return OS_ERR;
  }
  PDH_FMT_COUNTERVALUE counter_value;
  if (read_counter(_process_cpu_load, 0, PDH_FMT_DOUBLE | PDH_FMT_NOCAP100, &counter_value) != OS_OK) {
    return OS_ERR;
  }
  double process_load = counter_value.doubleValue / cpu_factor();
  process_load = MIN2<double>(1, process_load);
  process_load = MAX2<double>(0, process_load);
  *cpu_load = process_load;
  return OS_OK;
}

int CPUPerformanceInterface::CPUPerformance::cpu_loads_process(double* jvm_user_load,
                                                               double* jvm_kernel_load,
                                                               double* system_total_load) {
  assert(jvm_user_load != NULL, "jvm_user_load is NULL!");
  assert(jvm_kernel_load != NULL, "jvm_kernel_load is NULL!");
  assert(system_total_load != NULL, "system_total_load is NULL!");
  *jvm_user_load = .0;
  *jvm_kernel_load = .0;
  *system_total_load = .0;

  if (_process_cpu_load == NULL || !_process_cpu_load->set.initialized) {
    return OS_ERR;
  }
  if (collect(_process_cpu_load) != OS_OK) {
    return OS_ERR;
  }
  double process_load = .0;
  PDH_FMT_COUNTERVALUE counter_value;
  // Read PDH_PROCESSOR_TIME_IDX as counter_idx == 0
  if (read_counter(_process_cpu_load, 0, PDH_FMT_DOUBLE | PDH_FMT_NOCAP100, &counter_value) != OS_OK) {
    return OS_ERR;
  }
  process_load = counter_value.doubleValue / cpu_factor();
  process_load = MIN2<double>(1, process_load);
  process_load = MAX2<double>(0, process_load);
  // Read PDH_PRIV_PROCESSOR_TIME_IDX as counter_idx == 1
  if (read_counter(_process_cpu_load, 1, PDH_FMT_DOUBLE | PDH_FMT_NOCAP100, &counter_value) != OS_OK) {
    return OS_ERR;
  }
  double process_kernel_load = counter_value.doubleValue / cpu_factor();
  process_kernel_load = MIN2<double>(1, process_kernel_load);
  process_kernel_load = MAX2<double>(0, process_kernel_load);
  *jvm_kernel_load = process_kernel_load;

  double user_load = process_load - process_kernel_load;
  user_load = MIN2<double>(1, user_load);
  user_load = MAX2<double>(0, user_load);
  *jvm_user_load = user_load;
  if (collect(_machine_cpu_load) != OS_OK) {
    return OS_ERR;
  }
  // Read PDH_PROCESSOR_IDX as counter_idx == _machine_cpu_load->noOfCounters - 1
  if (read_counter(_machine_cpu_load, _machine_cpu_load->noOfCounters - 1, PDH_FMT_DOUBLE, &counter_value) != OS_OK) {
    return OS_ERR;
  }
  double machine_load = counter_value.doubleValue / 100;
  assert(machine_load >= 0, "machine_load is negative!");
  // clamp at user+system and 1.0
  if (*jvm_kernel_load + *jvm_user_load > machine_load) {
    machine_load = MIN2(*jvm_kernel_load + *jvm_user_load, 1.0);
  }
  *system_total_load = machine_load;
  return OS_OK;
}

int CPUPerformanceInterface::CPUPerformance::context_switch_rate(double* rate) {
  assert(rate != NULL, "invariant");
  *rate = .0;
  if (_context_switches == NULL || !_context_switches->initialized) {
    return OS_ERR;
  }
  if (collect(_context_switches) != OS_OK) {
    return OS_ERR;
  }
  PDH_FMT_COUNTERVALUE counter_value;
  if (read_counter(_context_switches, PDH_FMT_DOUBLE, &counter_value) != OS_OK) {
    return OS_ERR;
  }
  *rate = counter_value.doubleValue;
  return OS_OK;
}

class SystemProcessInterface::SystemProcesses : public CHeapObj<mtInternal> {
  friend class SystemProcessInterface;
 private:
  class ProcessIterator : public CHeapObj<mtInternal> {
    friend class SystemProcessInterface::SystemProcesses;
   private:
    HANDLE         _hProcessSnap;
    PROCESSENTRY32 _pe32;
    BOOL           _valid;
    char           _exePath[MAX_PATH];
    ProcessIterator();
    ~ProcessIterator();
    bool initialize();

    int current(SystemProcess* const process_info);
    int next_process();
    bool is_valid() const { return _valid != FALSE; }
    char* allocate_string(const char* str) const;
    int snapshot();
  };

  ProcessIterator* _iterator;
  SystemProcesses();
  ~SystemProcesses();
  bool initialize();

  // information about system processes
  int system_processes(SystemProcess** system_processes, int* no_of_sys_processes) const;
};

SystemProcessInterface::SystemProcesses::ProcessIterator::ProcessIterator() {
  _hProcessSnap = INVALID_HANDLE_VALUE;
  _valid = FALSE;
  _pe32.dwSize = sizeof(PROCESSENTRY32);
}

bool SystemProcessInterface::SystemProcesses::ProcessIterator::initialize() {
  return true;
}

int SystemProcessInterface::SystemProcesses::ProcessIterator::snapshot() {
  // take snapshot of all process in the system
  _hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
  if (_hProcessSnap == INVALID_HANDLE_VALUE) {
    return OS_ERR;
  }
  // step to first process
  _valid = Process32First(_hProcessSnap, &_pe32);
  return is_valid() ? OS_OK : OS_ERR;
}

SystemProcessInterface::SystemProcesses::ProcessIterator::~ProcessIterator() {
  if (_hProcessSnap != INVALID_HANDLE_VALUE) {
    CloseHandle(_hProcessSnap);
  }
}

int SystemProcessInterface::SystemProcesses::ProcessIterator::current(SystemProcess* process_info) {
  assert(is_valid(), "no current process to be fetched!");
  assert(process_info != NULL, "process_info is NULL!");
  char* exePath = NULL;
  HANDLE hProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false, _pe32.th32ProcessID);
  if (hProcess != NULL) {
    HMODULE hMod;
    DWORD cbNeeded;
    if (EnumProcessModules(hProcess, &hMod, sizeof(hMod), &cbNeeded) != 0) {
      if (GetModuleFileNameExA(hProcess, hMod, _exePath, sizeof(_exePath)) != 0) {
        exePath = _exePath;
      }
    }
    CloseHandle (hProcess);
  }
  process_info->set_pid((int)_pe32.th32ProcessID);
  process_info->set_name(allocate_string(_pe32.szExeFile));
  process_info->set_path(allocate_string(exePath));
  return OS_OK;
}

char* SystemProcessInterface::SystemProcesses::ProcessIterator::allocate_string(const char* str) const {
  return str != NULL ? os::strdup_check_oom(str, mtInternal) : NULL;
}

int SystemProcessInterface::SystemProcesses::ProcessIterator::next_process() {
  _valid = Process32Next(_hProcessSnap, &_pe32);
  return OS_OK;
}

SystemProcessInterface::SystemProcesses::SystemProcesses() : _iterator(NULL) {}

bool SystemProcessInterface::SystemProcesses::initialize() {
  _iterator = new SystemProcessInterface::SystemProcesses::ProcessIterator();
  return _iterator->initialize();
}

SystemProcessInterface::SystemProcesses::~SystemProcesses() {
  if (_iterator != NULL) {
    delete _iterator;
  }
}

int SystemProcessInterface::SystemProcesses::system_processes(SystemProcess** system_processes,
                                                              int* no_of_sys_processes) const {
  assert(system_processes != NULL, "system_processes pointer is NULL!");
  assert(no_of_sys_processes != NULL, "system_processes counter pointers is NULL!");
  assert(_iterator != NULL, "iterator is NULL!");

  // initialize pointers
  *no_of_sys_processes = 0;
  *system_processes = NULL;

  // take process snapshot
  if (_iterator->snapshot() != OS_OK) {
    return OS_ERR;
  }

  while (_iterator->is_valid()) {
    SystemProcess* tmp = new SystemProcess();
    _iterator->current(tmp);

    //if already existing head
    if (*system_processes != NULL) {
      //move "first to second"
      tmp->set_next(*system_processes);
    }
    // new head
    *system_processes = tmp;
    // increment
    (*no_of_sys_processes)++;
    // step forward
    _iterator->next_process();
  }
  return OS_OK;
}

int SystemProcessInterface::system_processes(SystemProcess** system_procs,
                                             int* no_of_sys_processes) const {
  return _impl->system_processes(system_procs, no_of_sys_processes);
}

SystemProcessInterface::SystemProcessInterface() : _impl(NULL) {}

bool SystemProcessInterface::initialize() {
  _impl = new SystemProcessInterface::SystemProcesses();
  return _impl->initialize();
}

SystemProcessInterface::~SystemProcessInterface() {
  if (_impl != NULL) {
    delete _impl;
  }
}

CPUInformationInterface::CPUInformationInterface() : _cpu_info(NULL) {}

bool CPUInformationInterface::initialize() {
  _cpu_info = new CPUInformation();
  _cpu_info->set_number_of_hardware_threads(VM_Version_Ext::number_of_threads());
  _cpu_info->set_number_of_cores(VM_Version_Ext::number_of_cores());
  _cpu_info->set_number_of_sockets(VM_Version_Ext::number_of_sockets());
  _cpu_info->set_cpu_name(VM_Version_Ext::cpu_name());
  _cpu_info->set_cpu_description(VM_Version_Ext::cpu_description());
  return true;
}

CPUInformationInterface::~CPUInformationInterface() {
  if (_cpu_info != NULL) {
    FREE_C_HEAP_ARRAY(char, _cpu_info->cpu_name());
    _cpu_info->set_cpu_name(NULL);
    FREE_C_HEAP_ARRAY(char, _cpu_info->cpu_description());
    _cpu_info->set_cpu_description(NULL);
    delete _cpu_info;
  }
}

int CPUInformationInterface::cpu_information(CPUInformation& cpu_info) {
  if (NULL == _cpu_info) {
    return OS_ERR;
  }
  cpu_info = *_cpu_info; // shallow copy assignment
  return OS_OK;
}

class NetworkPerformanceInterface::NetworkPerformance : public CHeapObj<mtInternal> {
  friend class NetworkPerformanceInterface;
 private:
  bool _iphlp_attached;

  NetworkPerformance();
  NONCOPYABLE(NetworkPerformance);
  bool initialize();
  ~NetworkPerformance();
  int network_utilization(NetworkInterface** network_interfaces) const;
};

NetworkPerformanceInterface::NetworkPerformance::NetworkPerformance() : _iphlp_attached(false) {}

bool NetworkPerformanceInterface::NetworkPerformance::initialize() {
  _iphlp_attached = IphlpDll::IphlpAttach();
  return _iphlp_attached;
}

NetworkPerformanceInterface::NetworkPerformance::~NetworkPerformance() {
  if (_iphlp_attached) {
    IphlpDll::IphlpDetach();
  }
}

int NetworkPerformanceInterface::NetworkPerformance::network_utilization(NetworkInterface** network_interfaces) const {
  MIB_IF_TABLE2* table;

  if (IphlpDll::GetIfTable2(&table) != NO_ERROR) {
    return OS_ERR;
  }

  NetworkInterface* ret = NULL;
  for (ULONG i = 0; i < table->NumEntries; ++i) {
    if (table->Table[i].InterfaceAndOperStatusFlags.FilterInterface) {
      continue;
    }

    char buf[256];
    if (WideCharToMultiByte(CP_UTF8, 0, table->Table[i].Description, -1, buf, sizeof(buf), NULL, NULL) == 0) {
      continue;
    }

    NetworkInterface* cur = new NetworkInterface(buf, table->Table[i].InOctets, table->Table[i].OutOctets, ret);
    ret = cur;
  }

  IphlpDll::FreeMibTable(table);
  *network_interfaces = ret;

  return OS_OK;
}

NetworkPerformanceInterface::NetworkPerformanceInterface() : _impl(NULL) {}

NetworkPerformanceInterface::~NetworkPerformanceInterface() {
  if (_impl != NULL) {
    delete _impl;
  }
}

bool NetworkPerformanceInterface::initialize() {
  _impl = new NetworkPerformanceInterface::NetworkPerformance();
  return _impl->initialize();
}

int NetworkPerformanceInterface::network_utilization(NetworkInterface** network_interfaces) const {
  return _impl->network_utilization(network_interfaces);
}
