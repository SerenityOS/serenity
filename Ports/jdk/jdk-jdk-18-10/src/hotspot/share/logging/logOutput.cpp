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
#include "logging/logFileStreamOutput.hpp"
#include "logging/logOutput.hpp"
#include "logging/logSelection.hpp"
#include "logging/logTagSet.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"

LogOutput::~LogOutput() {
  os::free(_config_string);
}

void LogOutput::describe(outputStream *out) {
  out->print("%s ", name());
  out->print_raw(config_string()); // raw printed because length might exceed O_BUFLEN

  bool has_decorator = false;
  char delimiter = ' ';
  for (size_t d = 0; d < LogDecorators::Count; d++) {
    LogDecorators::Decorator decorator = static_cast<LogDecorators::Decorator>(d);
    if (decorators().is_decorator(decorator)) {
      has_decorator = true;
      out->print("%c%s", delimiter, LogDecorators::name(decorator));
      delimiter = ',';
    }
  }
  if (!has_decorator) {
    out->print(" none");
  }
}

void LogOutput::set_config_string(const char* string) {
  os::free(_config_string);
  _config_string = os::strdup(string, mtLogging);
  _config_string_buffer_size = strlen(_config_string) + 1;
}

void LogOutput::add_to_config_string(const LogSelection& selection) {
  if (_config_string_buffer_size < InitialConfigBufferSize) {
    _config_string_buffer_size = InitialConfigBufferSize;
    _config_string = REALLOC_C_HEAP_ARRAY(char, _config_string, _config_string_buffer_size, mtLogging);
  }

  size_t offset = strlen(_config_string);
  if (offset > 0) {
    // Add commas in-between tag and level combinations in the config string
    _config_string[offset++] = ',';
  }

  for (;;) {
    int ret = selection.describe(_config_string + offset,
                                 _config_string_buffer_size - offset);
    if (ret == -1) {
      // Double the buffer size and retry
      _config_string_buffer_size *= 2;
      _config_string = REALLOC_C_HEAP_ARRAY(char, _config_string, _config_string_buffer_size, mtLogging);
      continue;
    }
    break;
  };
}


static int tag_cmp(const void *a, const void *b) {
  return static_cast<const LogTagType*>(a) - static_cast<const LogTagType*>(b);
}

static void sort_tags(LogTagType tags[LogTag::MaxTags]) {
  size_t ntags = 0;
  while (tags[ntags] != LogTag::__NO_TAG) {
    ntags++;
  }
  qsort(tags, ntags, sizeof(*tags), tag_cmp);
}

static const size_t MaxSubsets = 1 << LogTag::MaxTags;

// Fill result with all possible subsets of the given tag set. Empty set not included.
// For example, if tags is {gc, heap} then the result is {{gc}, {heap}, {gc, heap}}.
// (Arguments with default values are intended exclusively for recursive calls.)
static void generate_all_subsets_of(LogTagType result[MaxSubsets][LogTag::MaxTags],
                                    size_t* result_size,
                                    const LogTagType tags[LogTag::MaxTags],
                                    LogTagType subset[LogTag::MaxTags] = NULL,
                                    const size_t subset_size = 0,
                                    const size_t depth = 0) {
  assert(subset_size <= LogTag::MaxTags, "subset must never have more than MaxTags tags");
  assert(depth <= LogTag::MaxTags, "recursion depth overflow");

  if (subset == NULL) {
    assert(*result_size == 0, "outer (non-recursive) call expects result_size to be 0");
    // Make subset the first element in the result array initially
    subset = result[0];
  }
  assert((void*) subset >= &result[0] && (void*) subset <= &result[MaxSubsets - 1],
         "subset should always point to element in result");

  if (depth == LogTag::MaxTags || tags[depth] == LogTag::__NO_TAG) {
    if (subset_size == 0) {
      // Ignore empty subset
      return;
    }
    if (subset_size != LogTag::MaxTags) {
      subset[subset_size] = LogTag::__NO_TAG;
    }
    assert(*result_size < MaxSubsets, "subsets overflow");
    *result_size += 1;

    // Bump subset and copy over current state
    memcpy(result[*result_size], subset, sizeof(*subset) * LogTag::MaxTags);
    subset = result[*result_size];
    return;
  }

  // Recurse, excluding the tag of the current depth
  generate_all_subsets_of(result, result_size, tags, subset, subset_size, depth + 1);
  // ... and with it included
  subset[subset_size] = tags[depth];
  generate_all_subsets_of(result, result_size, tags, subset, subset_size + 1, depth + 1);
}

// Generate all possible selections (for the given level) based on the given tag set,
// and add them to the selections array (growing it as necessary).
static void add_selections(LogSelection** selections,
                           size_t* n_selections,
                           size_t* selections_cap,
                           const LogTagSet& tagset,
                           LogLevelType level) {
  LogTagType tags[LogTag::MaxTags] = { LogTag::__NO_TAG };
  for (size_t i = 0; i < tagset.ntags(); i++) {
    tags[i] = tagset.tag(i);
  }

  size_t n_subsets = 0;
  LogTagType subsets[MaxSubsets][LogTag::MaxTags];
  generate_all_subsets_of(subsets, &n_subsets, tags);

  for (size_t i = 0; i < n_subsets; i++) {
    // Always keep tags sorted
    sort_tags(subsets[i]);

    // Ignore subsets already represented in selections
    bool unique = true;
    for (size_t sel = 0; sel < *n_selections; sel++) {
      if (level == (*selections)[sel].level() && (*selections)[sel].consists_of(subsets[i])) {
        unique = false;
        break;
      }
    }
    if (!unique) {
      continue;
    }

    LogSelection exact_selection(subsets[i], false, level);
    LogSelection wildcard_selection(subsets[i], true, level);

    // Check if the two selections match any tag sets
    bool wildcard_match = false;
    bool exact_match = false;
    for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
      if (!wildcard_selection.selects(*ts)) {
        continue;
      }

      wildcard_match = true;
      if (exact_selection.selects(*ts)) {
        exact_match = true;
      }
      if (exact_match) {
        break;
      }
    }

    if (!wildcard_match && !exact_match) {
      continue;
    }

    // Ensure there's enough room for both wildcard_match and exact_match
    if (*n_selections + 2 > *selections_cap) {
      *selections_cap *= 2;
      *selections = REALLOC_C_HEAP_ARRAY(LogSelection, *selections, *selections_cap, mtLogging);
    }

    // Add found matching selections to the result array
    if (exact_match) {
      (*selections)[(*n_selections)++] = exact_selection;
    }
    if (wildcard_match) {
      (*selections)[(*n_selections)++] = wildcard_selection;
    }
  }
}

void LogOutput::update_config_string(const size_t on_level[LogLevel::Count]) {
  // Find the most common level (MCL)
  LogLevelType mcl = LogLevel::Off;
  size_t max = on_level[LogLevel::Off];
  for (LogLevelType l = LogLevel::First; l <= LogLevel::Last; l = static_cast<LogLevelType>(l + 1)) {
     if (on_level[l] > max) {
       mcl = l;
       max = on_level[l];
     }
  }

  // Always let the first part of each output's config string be "all=<MCL>"
  {
    char buf[64];
    jio_snprintf(buf, sizeof(buf), "all=%s", LogLevel::name(mcl));
    set_config_string(buf);
  }

  // If there are no deviating tag sets, we're done
  size_t deviating_tagsets = LogTagSet::ntagsets() - max;
  if (deviating_tagsets == 0) {
    return;
  }

  size_t n_selections = 0;
  size_t selections_cap = 4 * MaxSubsets; // Start with some reasonably large initial capacity
  LogSelection* selections = NEW_C_HEAP_ARRAY(LogSelection, selections_cap, mtLogging);

  size_t n_deviates = 0;
  const LogTagSet** deviates = NEW_C_HEAP_ARRAY(const LogTagSet*, deviating_tagsets, mtLogging);

  // Generate all possible selections involving the deviating tag sets
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    LogLevelType level = ts->level_for(this);
    if (level == mcl) {
      continue;
    }
    deviates[n_deviates++] = ts;
    add_selections(&selections, &n_selections, &selections_cap, *ts, level);
  }

  // Reduce deviates greedily, using the "best" selection at each step to reduce the number of deviating tag sets
  while (n_deviates > 0) {
    size_t prev_deviates = n_deviates;
    int max_score = 0;

    guarantee(n_selections > 0, "Cannot find maximal selection.");
    const LogSelection* best_selection = &selections[0];
    for (size_t i = 0; i < n_selections; i++) {

      // Give the selection a score based on how many deviating tag sets it selects (with correct level)
      int score = 0;
      for (size_t d = 0; d < n_deviates; d++) {
        if (selections[i].selects(*deviates[d]) && deviates[d]->level_for(this) == selections[i].level()) {
          score++;
        }
      }

      // Ignore selections with lower score than the current best even before subtracting mismatched selections
      if (score < max_score) {
        continue;
      }

      // Subtract from the score the number of tag sets it selects with an incorrect level
      for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
        if (selections[i].selects(*ts) && ts->level_for(this) != selections[i].level()) {
          score--;
        }
      }

      // Pick the selection with the best score, or in the case of a tie, the one with fewest tags
      if (score > max_score ||
          (score == max_score && selections[i].ntags() < best_selection->ntags())) {
        max_score = score;
        best_selection = &selections[i];
      }
    }

    add_to_config_string(*best_selection);

    // Remove all deviates that this selection covered
    for (size_t d = 0; d < n_deviates;) {
      if (deviates[d]->level_for(this) == best_selection->level() && best_selection->selects(*deviates[d])) {
        deviates[d] = deviates[--n_deviates];
        continue;
      }
      d++;
    }

    // Add back any new deviates that this selection added (no array growth since removed > added)
    for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
      if (ts->level_for(this) == best_selection->level() || !best_selection->selects(*ts)) {
        continue;
      }

      bool already_added = false;
      for (size_t dev = 0; dev < n_deviates; dev++) {
        if (deviates[dev] == ts) {
          already_added = true;
          break;
        }
      }
      if (already_added) {
        continue;
      }

      deviates[n_deviates++] = ts;
    }

    // Reset the selections and generate a new ones based on the updated deviating tag sets
    n_selections = 0;
    for (size_t d = 0; d < n_deviates; d++) {
      add_selections(&selections, &n_selections, &selections_cap, *deviates[d], deviates[d]->level_for(this));
    }

    assert(n_deviates < deviating_tagsets, "deviating tag set array overflow");
    assert(prev_deviates > n_deviates, "number of deviating tag sets must never grow");
  }
  FREE_C_HEAP_ARRAY(LogTagSet*, deviates);
  FREE_C_HEAP_ARRAY(Selection, selections);
}

