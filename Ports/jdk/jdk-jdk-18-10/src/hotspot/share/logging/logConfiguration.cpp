/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "logging/logAsyncWriter.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logDecorations.hpp"
#include "logging/logDecorators.hpp"
#include "logging/logDiagnosticCommand.hpp"
#include "logging/logFileOutput.hpp"
#include "logging/logOutput.hpp"
#include "logging/logSelectionList.hpp"
#include "logging/logStream.hpp"
#include "logging/logTagSet.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include "runtime/semaphore.hpp"
#include "utilities/globalDefinitions.hpp"

LogOutput** LogConfiguration::_outputs = NULL;
size_t      LogConfiguration::_n_outputs = 0;

LogConfiguration::UpdateListenerFunction* LogConfiguration::_listener_callbacks = NULL;
size_t      LogConfiguration::_n_listener_callbacks = 0;

// LogFileOutput is the default type of output, its type prefix should be used if no type was specified
static const char* implicit_output_prefix = LogFileOutput::Prefix;

// Stack object to take the lock for configuring the logging.
// Should only be held during the critical parts of the configuration
// (when calling configure_output or reading/modifying the outputs array).
// Thread must never block when holding this lock.
class ConfigurationLock : public StackObj {
 private:
  // Semaphore used as lock
  static Semaphore _semaphore;
  debug_only(static intx _locking_thread_id;)
 public:
  ConfigurationLock() {
    _semaphore.wait();
    debug_only(_locking_thread_id = os::current_thread_id());
  }
  ~ConfigurationLock() {
    debug_only(_locking_thread_id = -1);
    _semaphore.signal();
  }
  debug_only(static bool current_thread_has_lock();)
};

Semaphore ConfigurationLock::_semaphore(1);
#ifdef ASSERT
intx ConfigurationLock::_locking_thread_id = -1;
bool ConfigurationLock::current_thread_has_lock() {
  return _locking_thread_id == os::current_thread_id();
}
#endif

void LogConfiguration::post_initialize() {
  // Reset the reconfigured status of all outputs
  for (size_t i = 0; i < _n_outputs; i++) {
    _outputs[i]->_reconfigured = false;
  }

  LogDiagnosticCommand::registerCommand();
  Log(logging) log;
  if (log.is_info()) {
    log.info("Log configuration fully initialized.");
    log_develop_info(logging)("Develop logging is available.");

    LogStream info_stream(log.info());
    describe_available(&info_stream);

    LogStream debug_stream(log.debug());
    LogTagSet::list_all_tagsets(&debug_stream);

    ConfigurationLock cl;
    describe_current_configuration(&info_stream);
  }
}

void LogConfiguration::initialize(jlong vm_start_time) {
  LogFileOutput::set_file_name_parameters(vm_start_time);
  assert(_outputs == NULL, "Should not initialize _outputs before this function, initialize called twice?");
  _outputs = NEW_C_HEAP_ARRAY(LogOutput*, 2, mtLogging);
  _outputs[0] = &StdoutLog;
  _outputs[1] = &StderrLog;
  _n_outputs = 2;
}

void LogConfiguration::finalize() {
  disable_outputs();
  FREE_C_HEAP_ARRAY(LogOutput*, _outputs);
}

// Normalizes the given LogOutput name to type=name form.
// For example, foo, "foo", file="foo", will all be normalized to file=foo (no quotes, prefixed).
static bool normalize_output_name(const char* full_name, char* buffer, size_t len, outputStream* errstream) {
  const char* start_quote = strchr(full_name, '"');
  const char* equals = strchr(full_name, '=');
  const bool quoted = start_quote != NULL;
  const bool is_stdout_or_stderr = (strcmp(full_name, "stdout") == 0 || strcmp(full_name, "stderr") == 0);

  // ignore equals sign within quotes
  if (quoted && equals > start_quote) {
    equals = NULL;
  }

  const char* prefix = "";
  size_t prefix_len = 0;
  const char* name = full_name;
  if (equals != NULL) {
    // split on equals sign
    name = equals + 1;
    prefix = full_name;
    prefix_len = equals - full_name + 1;
  } else if (!is_stdout_or_stderr) {
    prefix = implicit_output_prefix;
    prefix_len = strlen(prefix);
  }
  size_t name_len = strlen(name);

  if (quoted) {
    const char* end_quote = strchr(start_quote + 1, '"');
    if (end_quote == NULL) {
      errstream->print_cr("Output name has opening quote but is missing a terminating quote.");
      return false;
    }
    if (start_quote != name || end_quote[1] != '\0') {
      errstream->print_cr("Output name can not be partially quoted."
                          " Either surround the whole name with quotation marks,"
                          " or do not use quotation marks at all.");
      return false;
    }
    // strip start and end quote
    name++;
    name_len -= 2;
  }

  int ret = jio_snprintf(buffer, len, "%.*s%.*s", prefix_len, prefix, name_len, name);
  assert(ret > 0, "buffer issue");
  return true;
}

size_t LogConfiguration::find_output(const char* name) {
  for (size_t i = 0; i < _n_outputs; i++) {
    if (strcmp(_outputs[i]->name(), name) == 0) {
      return i;
    }
  }
  return SIZE_MAX;
}

LogOutput* LogConfiguration::new_output(const char* name,
                                        const char* options,
                                        outputStream* errstream) {
  LogOutput* output;
  if (strncmp(name, LogFileOutput::Prefix, strlen(LogFileOutput::Prefix)) == 0) {
    output = new LogFileOutput(name);
  } else {
    errstream->print_cr("Unsupported log output type: %s", name);
    return NULL;
  }

  bool success = output->initialize(options, errstream);
  if (!success) {
    errstream->print_cr("Initialization of output '%s' using options '%s' failed.", name, options);
    delete output;
    return NULL;
  }
  return output;
}

size_t LogConfiguration::add_output(LogOutput* output) {
  size_t idx = _n_outputs++;
  _outputs = REALLOC_C_HEAP_ARRAY(LogOutput*, _outputs, _n_outputs, mtLogging);
  _outputs[idx] = output;
  return idx;
}

void LogConfiguration::delete_output(size_t idx) {
  assert(idx > 1 && idx < _n_outputs,
         "idx must be in range 1 < idx < _n_outputs, but idx = " SIZE_FORMAT
         " and _n_outputs = " SIZE_FORMAT, idx, _n_outputs);
  LogOutput* output = _outputs[idx];
  // Swap places with the last output and shrink the array
  _outputs[idx] = _outputs[--_n_outputs];
  _outputs = REALLOC_C_HEAP_ARRAY(LogOutput*, _outputs, _n_outputs, mtLogging);
  delete output;
}

// MT-SAFETY
//
// The ConfigurationLock guarantees that only one thread is performing reconfiguration. This function still needs
// to be MT-safe because logsites in other threads may be executing in parallel. Reconfiguration means unified
// logging allows users to dynamically change tags and decorators of a log output via DCMD(logDiagnosticCommand.hpp).
//
// A RCU-style synchronization 'wait_until_no_readers()' is used inside of 'ts->set_output_level(output, level)'
// if a setting has changed. It guarantees that all logs, either synchronous writes or enqueuing to the async buffer
// see the new tags and decorators. It's worth noting that the synchronization occurs even if the level does not change.
//
// LogDecorator is a set of decorators represented in a uint. ts->update_decorators(decorators) is a union of the
// current decorators and new_decorators. It's safe to do output->set_decorators(decorators) because new_decorators
// is a subset of relevant tagsets decorators. After updating output's decorators, it is still safe to shrink all
// decorators of tagsets.
//
void LogConfiguration::configure_output(size_t idx, const LogSelectionList& selections, const LogDecorators& decorators) {
  assert(ConfigurationLock::current_thread_has_lock(), "Must hold configuration lock to call this function.");
  assert(idx < _n_outputs, "Invalid index, idx = " SIZE_FORMAT " and _n_outputs = " SIZE_FORMAT, idx, _n_outputs);
  LogOutput* output = _outputs[idx];

  output->_reconfigured = true;

  size_t on_level[LogLevel::Count] = {0};

  bool enabled = false;
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    LogLevelType level = selections.level_for(*ts);

    // Ignore tagsets that do not, and will not log on the output
    if (!ts->has_output(output) && (level == LogLevel::NotMentioned || level == LogLevel::Off)) {
      on_level[LogLevel::Off]++;
      continue;
    }

    // Update decorators before adding/updating output level,
    // so that the tagset will have the necessary decorators when requiring them.
    if (level != LogLevel::Off) {
      ts->update_decorators(decorators);
    }

    // Set the new level, if it changed
    if (level != LogLevel::NotMentioned) {
      ts->set_output_level(output, level);
    } else {
      // Look up the previously set level for this output on this tagset
      level = ts->level_for(output);
    }

    if (level != LogLevel::Off) {
      // Keep track of whether or not the output is ever used by some tagset
      enabled = true;
    }

    // Track of the number of tag sets on each level
    on_level[level]++;
  }

  // For async logging we have to ensure that all enqueued messages, which may refer to previous decorators,
  // or a soon-to-be-deleted output, are written out first. The flush() call ensures this.
  AsyncLogWriter::flush();

  // It is now safe to set the new decorators for the actual output
  output->set_decorators(decorators);

  // Update the decorators on all tagsets to get rid of unused decorators
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    ts->update_decorators();
  }

  if (!enabled && idx > 1) {
    // Output is unused and should be removed, unless it is stdout/stderr (idx < 2)
    delete_output(idx);
    return;
  }

  output->update_config_string(on_level);
  assert(strlen(output->config_string()) > 0, "should always have a config description");
}

void LogConfiguration::disable_outputs() {
  size_t idx = _n_outputs;

  // Remove all outputs from all tagsets.
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    ts->disable_outputs();
  }

  // Handle 'jcmd VM.log disable' and JVM termination.
  // ts->disable_outputs() above has disabled all output_lists with RCU synchronization.
  // Therefore, no new logging message can enter the async buffer for the time being.
  // flush out all pending messages before LogOutput instances die.
  AsyncLogWriter::flush();

  while (idx > 0) {
    LogOutput* out = _outputs[--idx];
    // Delete the output unless stdout or stderr (idx 0 or 1)
    if (idx > 1) {
      delete_output(idx);
    } else {
      out->set_config_string("all=off");
    }
  }
}

void LogConfiguration::disable_logging() {
  ConfigurationLock cl;
  disable_outputs();
  // Update the decorators on all tagsets to get rid of unused decorators
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    ts->update_decorators();
  }
  notify_update_listeners();
}

void LogConfiguration::configure_stdout(LogLevelType level, int exact_match, ...) {
  size_t i;
  va_list ap;
  LogTagType tags[LogTag::MaxTags];
  va_start(ap, exact_match);
  for (i = 0; i < LogTag::MaxTags; i++) {
    LogTagType tag = static_cast<LogTagType>(va_arg(ap, int));
    tags[i] = tag;
    if (tag == LogTag::__NO_TAG) {
      assert(i > 0, "Must specify at least one tag!");
      break;
    }
  }
  assert(i < LogTag::MaxTags || static_cast<LogTagType>(va_arg(ap, int)) == LogTag::__NO_TAG,
         "Too many tags specified! Can only have up to " SIZE_FORMAT " tags in a tag set.", LogTag::MaxTags);
  va_end(ap);

  LogSelection selection(tags, !exact_match, level);
  assert(selection.tag_sets_selected() > 0,
         "configure_stdout() called with invalid/non-existing log selection");
  LogSelectionList list(selection);

  // Apply configuration to stdout (output #0), with the same decorators as before.
  ConfigurationLock cl;
  configure_output(0, list, _outputs[0]->decorators());
  notify_update_listeners();
}

bool LogConfiguration::parse_command_line_arguments(const char* opts) {
  char* copy = os::strdup_check_oom(opts, mtLogging);

  // Split the option string to its colon separated components.
  char* str = copy;
  char* substrings[4] = {0};
  for (int i = 0 ; i < 4; i++) {
    substrings[i] = str;

    // Find the next colon or quote
    char* next = strpbrk(str, ":\"");
#ifdef _WINDOWS
    // Skip over Windows paths such as "C:\..."
    // Handle both C:\... and file=C:\..."
    if (next != NULL && next[0] == ':' && next[1] == '\\') {
      if (next == str + 1 || (strncmp(str, "file=", 5) == 0)) {
        next = strpbrk(next + 1, ":\"");
      }
    }
#endif
    while (next != NULL && *next == '"') {
      char* end_quote = strchr(next + 1, '"');
      if (end_quote == NULL) {
        log_error(logging)("Missing terminating quote in -Xlog option '%s'", str);
        os::free(copy);
        return false;
      }
      // Keep searching after the quoted substring
      next = strpbrk(end_quote + 1, ":\"");
    }

    if (next != NULL) {
      *next = '\0';
      str = next + 1;
    } else {
      str = NULL;
      break;
    }
  }

  if (str != NULL) {
    log_warning(logging)("Ignoring excess -Xlog options: \"%s\"", str);
  }

  // Parse and apply the separated configuration options
  char* what = substrings[0];
  char* output = substrings[1];
  char* decorators = substrings[2];
  char* output_options = substrings[3];
  char errbuf[512];
  stringStream ss(errbuf, sizeof(errbuf));
  bool success = parse_log_arguments(output, what, decorators, output_options, &ss);

  if (ss.size() > 0) {
    // If it failed, log the error. If it didn't fail, but something was written
    // to the stream, log it as a warning.
    LogLevelType level = success ? LogLevel::Warning : LogLevel::Error;

    Log(logging) log;
    char* start = errbuf;
    char* end = strchr(start, '\n');
    assert(end != NULL, "line must end with newline '%s'", start);
    do {
      assert(start < errbuf + sizeof(errbuf) &&
             end < errbuf + sizeof(errbuf),
             "buffer overflow");
      *end = '\0';
      log.write(level, "%s", start);
      start = end + 1;
      end = strchr(start, '\n');
      assert(end != NULL || *start == '\0', "line must end with newline '%s'", start);
    } while (end != NULL);
  }

  os::free(copy);
  return success;
}

bool LogConfiguration::parse_log_arguments(const char* outputstr,
                                           const char* selectionstr,
                                           const char* decoratorstr,
                                           const char* output_options,
                                           outputStream* errstream) {
  assert(errstream != NULL, "errstream can not be NULL");
  if (outputstr == NULL || strlen(outputstr) == 0) {
    outputstr = "stdout";
  }

  LogSelectionList selections;
  if (!selections.parse(selectionstr, errstream)) {
    return false;
  }

  LogDecorators decorators;
  if (!decorators.parse(decoratorstr, errstream)) {
    return false;
  }

  ConfigurationLock cl;
  size_t idx;
  bool added = false;
  if (outputstr[0] == '#') { // Output specified using index
    int ret = sscanf(outputstr + 1, SIZE_FORMAT, &idx);
    if (ret != 1 || idx >= _n_outputs) {
      errstream->print_cr("Invalid output index '%s'", outputstr);
      return false;
    }
  } else { // Output specified using name
    // Normalize the name, stripping quotes and ensures it includes type prefix
    size_t len = strlen(outputstr) + strlen(implicit_output_prefix) + 1;
    char* normalized = NEW_C_HEAP_ARRAY(char, len, mtLogging);
    if (!normalize_output_name(outputstr, normalized, len, errstream)) {
      return false;
    }

    idx = find_output(normalized);
    if (idx == SIZE_MAX) {
      // Attempt to create and add the output
      LogOutput* output = new_output(normalized, output_options, errstream);
      if (output != NULL) {
        idx = add_output(output);
        added = true;
      }
    }

    FREE_C_HEAP_ARRAY(char, normalized);
    if (idx == SIZE_MAX) {
      return false;
    }
  }
  if (!added && output_options != NULL && strlen(output_options) > 0) {
    errstream->print_cr("Output options for existing outputs are ignored.");
  }
  configure_output(idx, selections, decorators);
  notify_update_listeners();
  selections.verify_selections(errstream);
  return true;
}

void LogConfiguration::describe_available(outputStream* out) {
  out->print("Available log levels:");
  for (size_t i = 0; i < LogLevel::Count; i++) {
    out->print("%s %s", (i == 0 ? "" : ","), LogLevel::name(static_cast<LogLevelType>(i)));
  }
  out->cr();

  out->print("Available log decorators:");
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    LogDecorators::Decorator d = static_cast<LogDecorators::Decorator>(i);
    out->print("%s %s (%s)", (i == 0 ? "" : ","), LogDecorators::name(d), LogDecorators::abbreviation(d));
  }
  out->cr();

  out->print("Available log tags:");
  LogTag::list_tags(out);

  LogTagSet::describe_tagsets(out);
}

void LogConfiguration::describe_current_configuration(outputStream* out) {
  out->print_cr("Log output configuration:");
  for (size_t i = 0; i < _n_outputs; i++) {
    out->print(" #" SIZE_FORMAT ": ", i);
    _outputs[i]->describe(out);
    if (_outputs[i]->is_reconfigured()) {
      out->print(" (reconfigured)");
    }
    out->cr();
  }
}

void LogConfiguration::describe(outputStream* out) {
  describe_available(out);
  ConfigurationLock cl;
  describe_current_configuration(out);
}

void LogConfiguration::print_command_line_help(outputStream* out) {
  out->print_cr("-Xlog Usage: -Xlog[:[selections][:[output][:[decorators][:output-options]]]]");
  out->print_cr("\t where 'selections' are combinations of tags and levels of the form tag1[+tag2...][*][=level][,...]");
  out->print_cr("\t NOTE: Unless wildcard (*) is specified, only log messages tagged with exactly the tags specified will be matched.");
  out->cr();

  out->print_cr("Available log levels:");
  for (size_t i = 0; i < LogLevel::Count; i++) {
    out->print("%s %s", (i == 0 ? "" : ","), LogLevel::name(static_cast<LogLevelType>(i)));
  }
  out->cr();
  out->cr();

  out->print_cr("Available log decorators: ");
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    LogDecorators::Decorator d = static_cast<LogDecorators::Decorator>(i);
    out->print("%s %s (%s)", (i == 0 ? "" : ","), LogDecorators::name(d), LogDecorators::abbreviation(d));
  }
  out->cr();
  out->print_cr(" Decorators can also be specified as 'none' for no decoration.");
  out->cr();

  out->print_cr("Available log tags:");
  LogTag::list_tags(out);
  out->print_cr(" Specifying 'all' instead of a tag combination matches all tag combinations.");
  out->cr();

  LogTagSet::describe_tagsets(out);

  out->print_cr("\nAvailable log outputs:");
  out->print_cr(" stdout/stderr");
  out->print_cr(" file=<filename>");
  out->print_cr("  If the filename contains %%p and/or %%t, they will expand to the JVM's PID and startup timestamp, respectively.");
  out->print_cr("  Additional output-options for file outputs:");
  out->print_cr("   filesize=..  - Target byte size for log rotation (supports K/M/G suffix)."
                                    " If set to 0, log rotation will not trigger automatically,"
                                    " but can be performed manually (see the VM.log DCMD).");
  out->print_cr("   filecount=.. - Number of files to keep in rotation (not counting the active file)."
                                    " If set to 0, log rotation is disabled."
                                    " This will cause existing log files to be overwritten.");
  out->cr();
  out->print_cr("\nAsynchronous logging (off by default):");
  out->print_cr(" -Xlog:async");
  out->print_cr("  All log messages are written to an intermediate buffer first and will then be flushed"
                " to the corresponding log outputs by a standalone thread. Write operations at logsites are"
                " guaranteed non-blocking.");
  out->cr();

  out->print_cr("Some examples:");
  out->print_cr(" -Xlog");
  out->print_cr("\t Log all messages up to 'info' level to stdout with 'uptime', 'levels' and 'tags' decorations.");
  out->print_cr("\t (Equivalent to -Xlog:all=info:stdout:uptime,levels,tags).");
  out->cr();

  out->print_cr(" -Xlog:gc");
  out->print_cr("\t Log messages tagged with 'gc' tag up to 'info' level to stdout, with default decorations.");
  out->cr();

  out->print_cr(" -Xlog:gc,safepoint");
  out->print_cr("\t Log messages tagged either with 'gc' or 'safepoint' tags, both up to 'info' level, to stdout, with default decorations.");
  out->print_cr("\t (Messages tagged with both 'gc' and 'safepoint' will not be logged.)");
  out->cr();

  out->print_cr(" -Xlog:gc+ref=debug");
  out->print_cr("\t Log messages tagged with both 'gc' and 'ref' tags, up to 'debug' level, to stdout, with default decorations.");
  out->print_cr("\t (Messages tagged only with one of the two tags will not be logged.)");
  out->cr();

  out->print_cr(" -Xlog:gc=debug:file=gc.txt:none");
  out->print_cr("\t Log messages tagged with 'gc' tag up to 'debug' level to file 'gc.txt' with no decorations.");
  out->cr();

  out->print_cr(" -Xlog:gc=trace:file=gctrace.txt:uptimemillis,pid:filecount=5,filesize=1m");
  out->print_cr("\t Log messages tagged with 'gc' tag up to 'trace' level to a rotating fileset of 5 files of size 1MB,");
  out->print_cr("\t using the base name 'gctrace.txt', with 'uptimemillis' and 'pid' decorations.");
  out->cr();

  out->print_cr(" -Xlog:gc::uptime,tid");
  out->print_cr("\t Log messages tagged with 'gc' tag up to 'info' level to output 'stdout', using 'uptime' and 'tid' decorations.");
  out->cr();

  out->print_cr(" -Xlog:gc*=info,safepoint*=off");
  out->print_cr("\t Log messages tagged with at least 'gc' up to 'info' level, but turn off logging of messages tagged with 'safepoint'.");
  out->print_cr("\t (Messages tagged with both 'gc' and 'safepoint' will not be logged.)");
  out->cr();

  out->print_cr(" -Xlog:disable -Xlog:safepoint=trace:safepointtrace.txt");
  out->print_cr("\t Turn off all logging, including warnings and errors,");
  out->print_cr("\t and then enable messages tagged with 'safepoint' up to 'trace' level to file 'safepointtrace.txt'.");

  out->print_cr(" -Xlog:async -Xlog:gc=debug:file=gc.log -Xlog:safepoint=trace");
  out->print_cr("\t Write logs asynchronously. Enable messages tagged with 'safepoint' up to 'trace' level to stdout ");
  out->print_cr("\t and messages tagged with 'gc' up to 'debug' level to file 'gc.log'.");
}

void LogConfiguration::rotate_all_outputs() {
  // Start from index 2 since neither stdout nor stderr can be rotated.
  for (size_t idx = 2; idx < _n_outputs; idx++) {
    _outputs[idx]->force_rotate();
  }
}

void LogConfiguration::register_update_listener(UpdateListenerFunction cb) {
  assert(cb != NULL, "Should not register NULL as listener");
  ConfigurationLock cl;
  size_t idx = _n_listener_callbacks++;
  _listener_callbacks = REALLOC_C_HEAP_ARRAY(UpdateListenerFunction,
                                             _listener_callbacks,
                                             _n_listener_callbacks,
                                             mtLogging);
  _listener_callbacks[idx] = cb;
}

void LogConfiguration::notify_update_listeners() {
  assert(ConfigurationLock::current_thread_has_lock(), "notify_update_listeners must be called in ConfigurationLock scope (lock held)");
  for (size_t i = 0; i < _n_listener_callbacks; i++) {
    _listener_callbacks[i]();
  }
}

bool LogConfiguration::_async_mode = false;
