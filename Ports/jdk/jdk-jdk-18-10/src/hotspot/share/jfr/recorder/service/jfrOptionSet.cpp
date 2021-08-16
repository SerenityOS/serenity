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
#include "classfile/javaClasses.hpp"
#include "jfr/dcmd/jfrDcmds.hpp"
#include "jfr/recorder/service/jfrMemorySizer.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/java.hpp"
#include "runtime/thread.inline.hpp"
#include "services/diagnosticArgument.hpp"
#include "services/diagnosticFramework.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"

struct ObsoleteOption {
  const char* name;
  const char* message;
};

static const ObsoleteOption OBSOLETE_OPTIONS[] = {
  {"checkpointbuffersize", ""},
  {"maxsize",              "Use -XX:StartFlightRecording:maxsize=... instead."},
  {"maxage",               "Use -XX:StartFlightRecording:maxage=... instead."},
  {"settings",             "Use -XX:StartFlightRecording:settings=... instead."},
  {"defaultrecording",     "Use -XX:StartFlightRecording:disk=false to create an in-memory recording."},
  {"disk",                 "Use -XX:StartFlightRecording:disk=... instead."},
  {"dumponexit",           "Use -XX:StartFlightRecording:dumponexit=... instead."},
  {"dumponexitpath",       "Use -XX:StartFlightRecording:filename=... instead."},
  {"loglevel",             "Use -Xlog:jfr=... instead."}
};

jlong JfrOptionSet::max_chunk_size() {
  return _max_chunk_size;
}

void JfrOptionSet::set_max_chunk_size(jlong value) {
  _max_chunk_size = value;
}

jlong JfrOptionSet::global_buffer_size() {
  return _global_buffer_size;
}

void JfrOptionSet::set_global_buffer_size(jlong value) {
  _global_buffer_size = value;
}

jlong JfrOptionSet::thread_buffer_size() {
  return _thread_buffer_size;
}

void JfrOptionSet::set_thread_buffer_size(jlong value) {
  _thread_buffer_size = value;
}

jlong JfrOptionSet::memory_size() {
  return _memory_size;
}

void JfrOptionSet::set_memory_size(jlong value) {
  _memory_size = value;
}

jlong JfrOptionSet::num_global_buffers() {
  return _num_global_buffers;
}

void JfrOptionSet::set_num_global_buffers(jlong value) {
  _num_global_buffers = value;
}

jint JfrOptionSet::old_object_queue_size() {
  return (jint)_old_object_queue_size;
}

void JfrOptionSet::set_old_object_queue_size(jlong value) {
  _old_object_queue_size = value;
}

u4 JfrOptionSet::stackdepth() {
  return _stack_depth;
}

void JfrOptionSet::set_stackdepth(u4 depth) {
  if (depth < MIN_STACK_DEPTH) {
    _stack_depth = MIN_STACK_DEPTH;
  } else if (depth > MAX_STACK_DEPTH) {
    _stack_depth = MAX_STACK_DEPTH;
  } else {
    _stack_depth = depth;
  }
}

bool JfrOptionSet::sample_threads() {
  return _sample_threads == JNI_TRUE;
}

void JfrOptionSet::set_sample_threads(jboolean sample) {
  _sample_threads = sample;
}

bool JfrOptionSet::can_retransform() {
  return _retransform == JNI_TRUE;
}

void JfrOptionSet::set_retransform(jboolean value) {
  _retransform = value;
}

bool JfrOptionSet::sample_protection() {
  return _sample_protection == JNI_TRUE;
}

#ifdef ASSERT
void JfrOptionSet::set_sample_protection(jboolean protection) {
  _sample_protection = protection;
}
#endif

bool JfrOptionSet::compressed_integers() {
  // Set this to false for debugging purposes.
  return true;
}

bool JfrOptionSet::allow_retransforms() {
#if INCLUDE_JVMTI
  return true;
#else
  return false;
#endif
}

bool JfrOptionSet::allow_event_retransforms() {
  return allow_retransforms() && (DumpSharedSpaces || can_retransform());
}

// default options for the dcmd parser
const char* const default_repository = NULL;
const char* const default_global_buffer_size = "512k";
const char* const default_num_global_buffers = "20";
const char* const default_memory_size = "10m";
const char* const default_thread_buffer_size = "8k";
const char* const default_max_chunk_size = "12m";
const char* const default_sample_threads = "true";
const char* const default_stack_depth = "64";
const char* const default_retransform = "true";
const char* const default_old_object_queue_size = "256";
DEBUG_ONLY(const char* const default_sample_protection = "false";)

// statics
static DCmdArgument<char*> _dcmd_repository(
  "repository",
  "Flight recorder disk repository location",
  "STRING",
  false,
  default_repository);

static DCmdArgument<MemorySizeArgument> _dcmd_threadbuffersize(
  "threadbuffersize",
  "Thread buffer size",
  "MEMORY SIZE",
  false,
  default_thread_buffer_size);

static DCmdArgument<MemorySizeArgument> _dcmd_memorysize(
  "memorysize",
  "Size of memory to be used by Flight Recorder",
  "MEMORY SIZE",
  false,
  default_memory_size);

static DCmdArgument<MemorySizeArgument> _dcmd_globalbuffersize(
  "globalbuffersize",
  "Global buffer size",
  "MEMORY SIZE",
  false,
  default_global_buffer_size);

static DCmdArgument<jlong> _dcmd_numglobalbuffers(
  "numglobalbuffers",
  "Number of global buffers",
  "JULONG",
  false,
  default_num_global_buffers);

static DCmdArgument<MemorySizeArgument> _dcmd_maxchunksize(
  "maxchunksize",
  "Maximum size of a single repository disk chunk",
  "MEMORY SIZE",
  false,
  default_max_chunk_size);

static DCmdArgument<jlong> _dcmd_old_object_queue_size (
  "old-object-queue-size",
  "Maximum number of old objects to track",
  "JINT",
  false,
  default_old_object_queue_size);

static DCmdArgument<bool> _dcmd_sample_threads(
  "samplethreads",
  "Thread sampling enable / disable (only sampling when event enabled and sampling enabled)",
  "BOOLEAN",
  false,
  default_sample_threads);

#ifdef ASSERT
static DCmdArgument<bool> _dcmd_sample_protection(
  "sampleprotection",
  "Safeguard for stackwalking while sampling threads (false by default)",
  "BOOLEAN",
  false,
  default_sample_protection);
#endif

static DCmdArgument<jlong> _dcmd_stackdepth(
  "stackdepth",
  "Stack depth for stacktraces (minimum 1, maximum 2048)",
  "JULONG",
  false,
  default_stack_depth);

static DCmdArgument<bool> _dcmd_retransform(
  "retransform",
  "If event classes should be instrumented using JVMTI (by default true)",
  "BOOLEAN",
  true,
  default_retransform);

static DCmdParser _parser;

static void register_parser_options() {
  _parser.add_dcmd_option(&_dcmd_repository);
  _parser.add_dcmd_option(&_dcmd_threadbuffersize);
  _parser.add_dcmd_option(&_dcmd_memorysize);
  _parser.add_dcmd_option(&_dcmd_globalbuffersize);
  _parser.add_dcmd_option(&_dcmd_numglobalbuffers);
  _parser.add_dcmd_option(&_dcmd_maxchunksize);
  _parser.add_dcmd_option(&_dcmd_stackdepth);
  _parser.add_dcmd_option(&_dcmd_sample_threads);
  _parser.add_dcmd_option(&_dcmd_retransform);
  _parser.add_dcmd_option(&_dcmd_old_object_queue_size);
  DEBUG_ONLY(_parser.add_dcmd_option(&_dcmd_sample_protection);)
}

static bool parse_flight_recorder_options_internal(TRAPS) {
  if (FlightRecorderOptions == NULL) {
    return true;
  }
  const size_t length = strlen((const char*)FlightRecorderOptions);
  CmdLine cmdline((const char*)FlightRecorderOptions, length, true);
  _parser.parse(&cmdline, ',', THREAD);
  if (HAS_PENDING_EXCEPTION) {
    for (int index = 0; index < 9; index++) {
      ObsoleteOption option = OBSOLETE_OPTIONS[index];
      const char* p = strstr((const char*)FlightRecorderOptions, option.name);
      const size_t option_length = strlen(option.name);
      if (p != NULL && p[option_length] == '=') {
        log_error(arguments) ("-XX:FlightRecorderOptions=%s=... has been removed. %s", option.name, option.message);
        return false;
      }
    }
    ResourceMark rm(THREAD);
    oop message = java_lang_Throwable::message(PENDING_EXCEPTION);
    if (message != NULL) {
      const char* msg = java_lang_String::as_utf8_string(message);
      log_error(arguments) ("%s", msg);
    }
    CLEAR_PENDING_EXCEPTION;
    return false;
  }
  return true;
}

jlong JfrOptionSet::_max_chunk_size = 0;
jlong JfrOptionSet::_global_buffer_size = 0;
jlong JfrOptionSet::_thread_buffer_size = 0;
jlong JfrOptionSet::_memory_size = 0;
jlong JfrOptionSet::_num_global_buffers = 0;
jlong JfrOptionSet::_old_object_queue_size = 0;
u4 JfrOptionSet::_stack_depth = STACK_DEPTH_DEFAULT;
jboolean JfrOptionSet::_sample_threads = JNI_TRUE;
jboolean JfrOptionSet::_retransform = JNI_TRUE;
#ifdef ASSERT
jboolean JfrOptionSet::_sample_protection = JNI_FALSE;
#else
jboolean JfrOptionSet::_sample_protection = JNI_TRUE;
#endif

bool JfrOptionSet::initialize(JavaThread* thread) {
  register_parser_options();
  if (!parse_flight_recorder_options_internal(thread)) {
    return false;
  }
  if (_dcmd_retransform.is_set()) {
    set_retransform(_dcmd_retransform.value());
  }
  set_old_object_queue_size(_dcmd_old_object_queue_size.value());
  return adjust_memory_options();
}

bool JfrOptionSet::configure(TRAPS) {
  if (FlightRecorderOptions == NULL) {
    return true;
  }
  ResourceMark rm(THREAD);
  bufferedStream st;
  // delegate to DCmd execution
  JfrConfigureFlightRecorderDCmd configure(&st, false);
  configure._repository_path.set_is_set(_dcmd_repository.is_set());
  char* repo = _dcmd_repository.value();
  if (repo != NULL) {
    const size_t len = strlen(repo);
    char* repo_copy = JfrCHeapObj::new_array<char>(len + 1);
    if (NULL == repo_copy) {
      return false;
    }
    strncpy(repo_copy, repo, len + 1);
    configure._repository_path.set_value(repo_copy);
  }

  configure._stack_depth.set_is_set(_dcmd_stackdepth.is_set());
  configure._stack_depth.set_value(_dcmd_stackdepth.value());

  configure._thread_buffer_size.set_is_set(_dcmd_threadbuffersize.is_set());
  configure._thread_buffer_size.set_value(_dcmd_threadbuffersize.value());

  configure._global_buffer_count.set_is_set(_dcmd_numglobalbuffers.is_set());
  configure._global_buffer_count.set_value(_dcmd_numglobalbuffers.value());

  configure._global_buffer_size.set_is_set(_dcmd_globalbuffersize.is_set());
  configure._global_buffer_size.set_value(_dcmd_globalbuffersize.value());

  configure._max_chunk_size.set_is_set(_dcmd_maxchunksize.is_set());
  configure._max_chunk_size.set_value(_dcmd_maxchunksize.value());

  configure._memory_size.set_is_set(_dcmd_memorysize.is_set());
  configure._memory_size.set_value(_dcmd_memorysize.value());

  configure._sample_threads.set_is_set(_dcmd_sample_threads.is_set());
  configure._sample_threads.set_value(_dcmd_sample_threads.value());

  configure.set_verbose(false);
  configure.execute(DCmd_Source_Internal, THREAD);

  if (HAS_PENDING_EXCEPTION) {
    java_lang_Throwable::print(PENDING_EXCEPTION, tty);
    CLEAR_PENDING_EXCEPTION;
    return false;
  }
  return true;
}

template <typename Argument>
static julong divide_with_user_unit(Argument& memory_argument, julong value) {
  if (memory_argument.value()._size != memory_argument.value()._val) {
    switch (memory_argument.value()._multiplier) {
    case 'k': case 'K':
      return value / K;
    case 'm': case 'M':
      return value / M;
    case 'g': case 'G':
      return value / G;
    }
  }
  return value;
}

static const char higher_than_msg[] = "This value is higher than the maximum size limited ";
static const char lower_than_msg[] = "This value is lower than the minimum size required ";
template <typename Argument, bool lower>
static void log_out_of_range_value(Argument& memory_argument, julong min_value) {
  const char* msg = lower ? lower_than_msg : higher_than_msg;
  if (memory_argument.value()._size != memory_argument.value()._val) {
    // has multiplier
    log_error(arguments) (
      "%s" JULONG_FORMAT "%c", msg,
      divide_with_user_unit(memory_argument, min_value),
      memory_argument.value()._multiplier);
    return;
  }
  log_error(arguments) (
    "%s" JULONG_FORMAT, msg,
    divide_with_user_unit(memory_argument, min_value));
}

static const char default_val_msg[] = "Value default for option ";
static const char specified_val_msg[] = "Value specified for option ";
template <typename Argument>
static void log_set_value(Argument& memory_argument) {
  if (memory_argument.value()._size != memory_argument.value()._val) {
    // has multiplier
    log_error(arguments) (
      "%s\"%s\" is " JULONG_FORMAT "%c",
      memory_argument.is_set() ? specified_val_msg: default_val_msg,
      memory_argument.name(),
      memory_argument.value()._val,
      memory_argument.value()._multiplier);
    return;
  }
  log_error(arguments) (
    "%s\"%s\" is " JULONG_FORMAT,
    memory_argument.is_set() ? specified_val_msg: default_val_msg,
    memory_argument.name(), memory_argument.value()._val);
}

template <typename MemoryArg>
static void log_adjustments(MemoryArg& original_memory_size, julong new_memory_size, const char* msg) {
  log_trace(arguments) (
    "%s size (original) " JULONG_FORMAT " B (user defined: %s)",
    msg,
    original_memory_size.value()._size,
    original_memory_size.is_set() ? "true" : "false");
  log_trace(arguments) (
    "%s size (adjusted) " JULONG_FORMAT " B (modified: %s)",
    msg,
    new_memory_size,
    original_memory_size.value()._size != new_memory_size ? "true" : "false");
  log_trace(arguments) (
    "%s size (adjustment) %s" JULONG_FORMAT " B",
    msg,
    new_memory_size < original_memory_size.value()._size ? "-" : "+",
    new_memory_size < original_memory_size.value()._size ?
    original_memory_size.value()._size - new_memory_size :
    new_memory_size - original_memory_size.value()._size);
}

// All "triangular" options are explicitly set
// check that they are congruent and not causing
// an ambiguous situtation
template <typename MemoryArg, typename NumberArg>
static bool check_for_ambiguity(MemoryArg& memory_size, MemoryArg& global_buffer_size, NumberArg& num_global_buffers) {
  assert(memory_size.is_set(), "invariant");
  assert(global_buffer_size.is_set(), "invariant");
  assert(num_global_buffers.is_set(), "invariant");
  const julong calc_size = global_buffer_size.value()._size * (julong)num_global_buffers.value();
  if (calc_size != memory_size.value()._size) {
    // ambiguous
    log_set_value(global_buffer_size);
    log_error(arguments) (
      "Value specified for option \"%s\" is " JLONG_FORMAT,
      num_global_buffers.name(), num_global_buffers.value());
    log_set_value(memory_size);
    log_error(arguments) (
      "These values are causing an ambiguity when trying to determine how much memory to use");
    log_error(arguments) ("\"%s\" * \"%s\" do not equal \"%s\"",
      global_buffer_size.name(),
      num_global_buffers.name(),
      memory_size.name());
    log_error(arguments) (
      "Try to remove one of the involved options or make sure they are unambigous");
    return false;
  }
  return true;
}

template <typename Argument>
static bool ensure_minimum_count(Argument& buffer_count_argument, jlong min_count) {
  if (buffer_count_argument.value() < min_count) {
    log_error(arguments) (
      "Value specified for option \"%s\" is " JLONG_FORMAT,
      buffer_count_argument.name(), buffer_count_argument.value());
    log_error(arguments) (
      "This value is lower than the minimum required number " JLONG_FORMAT,
      min_count);
    return false;
  }
  return true;
}

// global buffer size and num global buffers specified
// ensure that particular combination to be ihigher than minimum memory size
template <typename MemoryArg, typename NumberArg>
static bool ensure_calculated_gteq(MemoryArg& global_buffer_size, NumberArg& num_global_buffers, julong min_value) {
  assert(global_buffer_size.is_set(), "invariant");
  assert(num_global_buffers.is_set(), "invariant");
  const julong calc_size = global_buffer_size.value()._size * (julong)num_global_buffers.value();
  if (calc_size < min_value) {
    log_set_value(global_buffer_size);
    log_error(arguments) (
      "Value specified for option \"%s\" is " JLONG_FORMAT,
      num_global_buffers.name(), num_global_buffers.value());
    log_error(arguments) ("\"%s\" * \"%s\" (" JULONG_FORMAT
      ") is lower than minimum memory size required " JULONG_FORMAT,
      global_buffer_size.name(),
      num_global_buffers.name(),
      calc_size,
      min_value);
    return false;
  }
  return true;
}

template <typename Argument>
static bool ensure_first_gteq_second(Argument& first_argument, Argument& second_argument) {
  if (second_argument.value()._size > first_argument.value()._size) {
    log_set_value(first_argument);
    log_set_value(second_argument);
    log_error(arguments) (
      "The value for option \"%s\" should not be larger than the value specified for option \"%s\"",
      second_argument.name(), first_argument.name());
    return false;
  }
  return true;
}

static bool valid_memory_relations(const JfrMemoryOptions& options) {
  if (options.global_buffer_size_configured) {
    if (options.memory_size_configured) {
      if (!ensure_first_gteq_second(_dcmd_memorysize, _dcmd_globalbuffersize)) {
        return false;
      }
    }
    if (options.thread_buffer_size_configured) {
      if (!ensure_first_gteq_second(_dcmd_globalbuffersize, _dcmd_threadbuffersize)) {
        return false;
      }
    }
    if (options.buffer_count_configured) {
      if (!ensure_calculated_gteq(_dcmd_globalbuffersize, _dcmd_numglobalbuffers, MIN_MEMORY_SIZE)) {
        return false;
      }
    }
  } else if (options.thread_buffer_size_configured && options.memory_size_configured) {
    if (!ensure_first_gteq_second(_dcmd_memorysize, _dcmd_threadbuffersize)) {
      return false;
    }
  }
  return true;
}

static void post_process_adjusted_memory_options(const JfrMemoryOptions& options) {
  assert(options.memory_size >= MIN_MEMORY_SIZE, "invariant");
  assert(options.global_buffer_size >= MIN_GLOBAL_BUFFER_SIZE, "invariant");
  assert(options.buffer_count >= MIN_BUFFER_COUNT, "invariant");
  assert(options.thread_buffer_size >= MIN_THREAD_BUFFER_SIZE, "invariant");
  log_adjustments(_dcmd_memorysize, options.memory_size, "Memory");
  log_adjustments(_dcmd_globalbuffersize, options.global_buffer_size, "Global buffer");
  log_adjustments(_dcmd_threadbuffersize, options.thread_buffer_size, "Thread local buffer");
  log_trace(arguments) ("Number of global buffers (original) " JLONG_FORMAT " (user defined: %s)",
    _dcmd_numglobalbuffers.value(),
    _dcmd_numglobalbuffers.is_set() ? "true" : "false");
  log_trace(arguments) ( "Number of global buffers (adjusted) " JULONG_FORMAT " (modified: %s)",
    options.buffer_count,
    _dcmd_numglobalbuffers.value() != (jlong)options.buffer_count ? "true" : "false");
  log_trace(arguments) ("Number of global buffers (adjustment) %s" JLONG_FORMAT,
    (jlong)options.buffer_count < _dcmd_numglobalbuffers.value() ? "" : "+",
    (jlong)options.buffer_count - _dcmd_numglobalbuffers.value());

  MemorySizeArgument adjusted_memory_size;
  adjusted_memory_size._val = divide_with_user_unit(_dcmd_memorysize, options.memory_size);
  adjusted_memory_size._multiplier = _dcmd_memorysize.value()._multiplier;
  adjusted_memory_size._size = options.memory_size;

  MemorySizeArgument adjusted_global_buffer_size;
  adjusted_global_buffer_size._val = divide_with_user_unit(_dcmd_globalbuffersize, options.global_buffer_size);
  adjusted_global_buffer_size._multiplier = _dcmd_globalbuffersize.value()._multiplier;
  adjusted_global_buffer_size._size = options.global_buffer_size;

  MemorySizeArgument adjusted_thread_buffer_size;
  adjusted_thread_buffer_size._val = divide_with_user_unit(_dcmd_threadbuffersize, options.thread_buffer_size);
  adjusted_thread_buffer_size._multiplier = _dcmd_threadbuffersize.value()._multiplier;
  adjusted_thread_buffer_size._size = options.thread_buffer_size;

  // store back to dcmd
  _dcmd_memorysize.set_value(adjusted_memory_size);
  _dcmd_memorysize.set_is_set(true);
  _dcmd_globalbuffersize.set_value(adjusted_global_buffer_size);
  _dcmd_globalbuffersize.set_is_set(true);
  _dcmd_numglobalbuffers.set_value((jlong)options.buffer_count);
  _dcmd_numglobalbuffers.set_is_set(true);
  _dcmd_threadbuffersize.set_value(adjusted_thread_buffer_size);
  _dcmd_threadbuffersize.set_is_set(true);
}

static void initialize_memory_options_from_dcmd(JfrMemoryOptions& options) {
  options.memory_size = _dcmd_memorysize.value()._size;
  options.global_buffer_size = MAX2<julong>(_dcmd_globalbuffersize.value()._size, (julong)os::vm_page_size());
  options.buffer_count = (julong)_dcmd_numglobalbuffers.value();
  options.thread_buffer_size = MAX2<julong>(_dcmd_threadbuffersize.value()._size, (julong)os::vm_page_size());
  // determine which options have been explicitly set
  options.memory_size_configured = _dcmd_memorysize.is_set();
  options.global_buffer_size_configured = _dcmd_globalbuffersize.is_set();
  options.buffer_count_configured = _dcmd_numglobalbuffers.is_set();
  options.thread_buffer_size_configured = _dcmd_threadbuffersize.is_set();
  assert(options.memory_size >= MIN_MEMORY_SIZE, "invariant");
  assert(options.global_buffer_size >= MIN_GLOBAL_BUFFER_SIZE, "invariant");
  assert(options.buffer_count >= MIN_BUFFER_COUNT, "invariant");
  assert(options.thread_buffer_size >= MIN_THREAD_BUFFER_SIZE, "invariant");
}

template <typename Argument>
static bool ensure_gteq(Argument& memory_argument, const jlong value) {
  if ((jlong)memory_argument.value()._size < value) {
    log_set_value(memory_argument);
    log_out_of_range_value<Argument, true>(memory_argument, value);
    return false;
  }
  return true;
}

static bool ensure_valid_minimum_sizes() {
  // ensure valid minimum memory sizes
  if (_dcmd_memorysize.is_set()) {
    if (!ensure_gteq(_dcmd_memorysize, MIN_MEMORY_SIZE)) {
      return false;
    }
  }
  if (_dcmd_globalbuffersize.is_set()) {
    if (!ensure_gteq(_dcmd_globalbuffersize, MIN_GLOBAL_BUFFER_SIZE)) {
      return false;
    }
  }
  if (_dcmd_numglobalbuffers.is_set()) {
    if (!ensure_minimum_count(_dcmd_numglobalbuffers, MIN_BUFFER_COUNT)) {
      return false;
    }
  }
  if (_dcmd_threadbuffersize.is_set()) {
    if (!ensure_gteq(_dcmd_threadbuffersize, MIN_THREAD_BUFFER_SIZE)) {
      return false;
    }
  }
  return true;
}

template <typename Argument>
static bool ensure_lteq(Argument& memory_argument, const jlong value) {
  if ((jlong)memory_argument.value()._size > value) {
    log_set_value(memory_argument);
    log_out_of_range_value<Argument, false>(memory_argument, value);
    return false;
  }
  return true;
}

static bool ensure_valid_maximum_sizes() {
  if (_dcmd_globalbuffersize.is_set()) {
    if (!ensure_lteq(_dcmd_globalbuffersize, MAX_GLOBAL_BUFFER_SIZE)) {
      return false;
    }
  }
  if (_dcmd_threadbuffersize.is_set()) {
    if (!ensure_lteq(_dcmd_threadbuffersize, MAX_THREAD_BUFFER_SIZE)) {
      return false;
    }
  }
  return true;
}

/**
 * Starting with the initial set of memory values from the user,
 * sanitize, enforce min/max rules and adjust to a set of consistent options.
 *
 * Adjusted memory sizes will be page aligned.
 */
bool JfrOptionSet::adjust_memory_options() {
  if (!ensure_valid_minimum_sizes() || !ensure_valid_maximum_sizes()) {
    return false;
  }
  JfrMemoryOptions options;
  initialize_memory_options_from_dcmd(options);
  if (!valid_memory_relations(options)) {
    return false;
  }
  if (!JfrMemorySizer::adjust_options(&options)) {
    if (options.buffer_count < MIN_BUFFER_COUNT || options.global_buffer_size < options.thread_buffer_size) {
      log_set_value(_dcmd_memorysize);
      log_set_value(_dcmd_globalbuffersize);
      log_error(arguments) ("%s \"%s\" is " JLONG_FORMAT,
        _dcmd_numglobalbuffers.is_set() ? specified_val_msg: default_val_msg,
        _dcmd_numglobalbuffers.name(), _dcmd_numglobalbuffers.value());
      log_set_value(_dcmd_threadbuffersize);
      if (options.buffer_count < MIN_BUFFER_COUNT) {
        log_error(arguments) ("numglobalbuffers " JULONG_FORMAT " is less than minimal value " JULONG_FORMAT,
          options.buffer_count, MIN_BUFFER_COUNT);
        log_error(arguments) ("Decrease globalbuffersize/threadbuffersize or increase memorysize");
      } else {
        log_error(arguments) ("globalbuffersize " JULONG_FORMAT " is less than threadbuffersize" JULONG_FORMAT,
          options.global_buffer_size, options.thread_buffer_size);
        log_error(arguments) ("Decrease globalbuffersize or increase memorysize or adjust global/threadbuffersize");
      }
      return false;
    }
    if (!check_for_ambiguity(_dcmd_memorysize, _dcmd_globalbuffersize, _dcmd_numglobalbuffers)) {
      return false;
    }
  }
  post_process_adjusted_memory_options(options);
  return true;
}

bool JfrOptionSet::parse_flight_recorder_option(const JavaVMOption** option, char* delimiter) {
  assert(option != NULL, "invariant");
  assert(delimiter != NULL, "invariant");
  assert((*option)->optionString != NULL, "invariant");
  assert(strncmp((*option)->optionString, "-XX:FlightRecorderOptions", 25) == 0, "invariant");
  if (*delimiter == '\0') {
    // -XX:FlightRecorderOptions without any delimiter and values
  } else {
    // -XX:FlightRecorderOptions[=|:]
    // set delimiter to '='
    *delimiter = '=';
  }
  return false;
}

static GrowableArray<const char*>* start_flight_recording_options_array = NULL;

bool JfrOptionSet::parse_start_flight_recording_option(const JavaVMOption** option, char* delimiter) {
  assert(option != NULL, "invariant");
  assert(delimiter != NULL, "invariant");
  assert((*option)->optionString != NULL, "invariant");
  assert(strncmp((*option)->optionString, "-XX:StartFlightRecording", 24) == 0, "invariant");
  const char* value = NULL;
  if (*delimiter == '\0') {
    // -XX:StartFlightRecording without any delimiter and values
    // Add dummy value "dumponexit=false" so -XX:StartFlightRecording can be used without explicit values.
    // The existing option->optionString points to stack memory so no need to deallocate.
    const_cast<JavaVMOption*>(*option)->optionString = (char*)"-XX:StartFlightRecording=dumponexit=false";
    value = (*option)->optionString + 25;
  } else {
    // -XX:StartFlightRecording[=|:]
    // set delimiter to '='
    *delimiter = '=';
    value = delimiter + 1;
  }
  assert(value != NULL, "invariant");
  const size_t value_length = strlen(value);

  if (start_flight_recording_options_array == NULL) {
    start_flight_recording_options_array = new (ResourceObj::C_HEAP, mtTracing) GrowableArray<const char*>(8, mtTracing);
  }
  assert(start_flight_recording_options_array != NULL, "invariant");
  char* const startup_value = NEW_C_HEAP_ARRAY(char, value_length + 1, mtTracing);
  strncpy(startup_value, value, value_length + 1);
  assert(strncmp(startup_value, value, value_length) == 0, "invariant");
  start_flight_recording_options_array->append(startup_value);
  return false;
}

const GrowableArray<const char*>* JfrOptionSet::start_flight_recording_options() {
  return start_flight_recording_options_array;
}

void JfrOptionSet::release_start_flight_recording_options() {
  if (start_flight_recording_options_array != NULL) {
    const int length = start_flight_recording_options_array->length();
    for (int i = 0; i < length; ++i) {
      FREE_C_HEAP_ARRAY(char, start_flight_recording_options_array->at(i));
    }
    delete start_flight_recording_options_array;
    start_flight_recording_options_array = NULL;
  }
}
