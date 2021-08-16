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
#ifndef SHARE_LOGGING_LOGTAG_HPP
#define SHARE_LOGGING_LOGTAG_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

// List of available logging tags. New tags should be added here, in
// alphabetical order.
// (The tags 'all', 'disable' and 'help' are special tags that can
// not be used in log calls, and should not be listed below.)
#define LOG_TAG_LIST \
  LOG_TAG(add) \
  LOG_TAG(age) \
  LOG_TAG(alloc) \
  LOG_TAG(annotation) \
  LOG_TAG(arguments) \
  LOG_TAG(attach) \
  LOG_TAG(barrier) \
  LOG_TAG(blocks) \
  LOG_TAG(bot) \
  LOG_TAG(breakpoint) \
  LOG_TAG(bytecode) \
  LOG_TAG(cds) \
  LOG_TAG(census) \
  LOG_TAG(class) \
  LOG_TAG(classhisto) \
  LOG_TAG(cleanup) \
  LOG_TAG(codecache) \
  NOT_PRODUCT(LOG_TAG(codestrings)) \
  LOG_TAG(compaction) \
  LOG_TAG(compilation) \
  LOG_TAG(condy) \
  LOG_TAG(constantpool) \
  LOG_TAG(constraints) \
  LOG_TAG(container) \
  LOG_TAG(coops) \
  LOG_TAG(cpu) \
  LOG_TAG(cset) \
  LOG_TAG(data) \
  LOG_TAG(datacreation) \
  LOG_TAG(dcmd) \
  LOG_TAG(decoder) \
  LOG_TAG(defaultmethods) \
  LOG_TAG(director) \
  LOG_TAG(dump) \
  LOG_TAG(dynamic) \
  LOG_TAG(ergo) \
  LOG_TAG(event) \
  LOG_TAG(exceptions) \
  LOG_TAG(exit) \
  LOG_TAG(fingerprint) \
  DEBUG_ONLY(LOG_TAG(foreign)) \
  LOG_TAG(free) \
  LOG_TAG(freelist) \
  LOG_TAG(gc) \
  NOT_PRODUCT(LOG_TAG(generate)) \
  LOG_TAG(handshake) \
  LOG_TAG(hashtables) \
  LOG_TAG(heap) \
  NOT_PRODUCT(LOG_TAG(heapsampling)) \
  LOG_TAG(humongous) \
  LOG_TAG(ihop) \
  LOG_TAG(iklass) \
  LOG_TAG(indy) \
  LOG_TAG(init) \
  LOG_TAG(inlining) \
  LOG_TAG(install) \
  LOG_TAG(interpreter) \
  LOG_TAG(itables) \
  LOG_TAG(jfr) \
  LOG_TAG(jit) \
  LOG_TAG(jni) \
  LOG_TAG(jvmci) \
  LOG_TAG(jvmti) \
  LOG_TAG(lambda) \
  LOG_TAG(library) \
  LOG_TAG(liveness) \
  LOG_TAG(load) /* Trace all classes loaded */ \
  LOG_TAG(loader) \
  LOG_TAG(logging) \
  LOG_TAG(malloc) \
  LOG_TAG(map) \
  LOG_TAG(mark) \
  LOG_TAG(marking) \
  LOG_TAG(membername) \
  LOG_TAG(memops) \
  LOG_TAG(metadata) \
  LOG_TAG(metaspace) \
  LOG_TAG(methodcomparator) \
  LOG_TAG(methodhandles) \
  LOG_TAG(mirror) \
  LOG_TAG(mmu) \
  LOG_TAG(module) \
  LOG_TAG(monitorinflation) \
  LOG_TAG(monitormismatch) \
  LOG_TAG(nestmates) \
  LOG_TAG(nmethod) \
  LOG_TAG(nmt) \
  LOG_TAG(normalize) \
  LOG_TAG(numa) \
  LOG_TAG(objecttagging) \
  LOG_TAG(obsolete) \
  LOG_TAG(oldobject) \
  LOG_TAG(oom) \
  LOG_TAG(oopmap) \
  LOG_TAG(oops) \
  LOG_TAG(oopstorage) \
  LOG_TAG(os) \
  LOG_TAG(owner) \
  LOG_TAG(pagesize) \
  LOG_TAG(parser) \
  LOG_TAG(patch) \
  LOG_TAG(path) \
  LOG_TAG(perf) \
  LOG_TAG(periodic) \
  LOG_TAG(phases) \
  LOG_TAG(plab) \
  LOG_TAG(placeholders) \
  LOG_TAG(preorder)  /* Trace all classes loaded in order referenced (not loaded) */ \
  LOG_TAG(preview)   /* Trace loading of preview feature types */ \
  LOG_TAG(promotion) \
  LOG_TAG(protectiondomain) /* "Trace protection domain verification" */ \
  LOG_TAG(ptrqueue) \
  LOG_TAG(purge) \
  LOG_TAG(record) \
  LOG_TAG(redefine) \
  LOG_TAG(ref) \
  LOG_TAG(refine) \
  LOG_TAG(region) \
  LOG_TAG(reloc) \
  LOG_TAG(remset) \
  LOG_TAG(resolve) \
  LOG_TAG(safepoint) \
  LOG_TAG(sampling) \
  LOG_TAG(scavenge) \
  LOG_TAG(sealed) \
  LOG_TAG(setting) \
  LOG_TAG(smr) \
  LOG_TAG(stackbarrier) \
  LOG_TAG(stackmap) \
  LOG_TAG(stacktrace) \
  LOG_TAG(stackwalk) \
  LOG_TAG(start) \
  LOG_TAG(startup) \
  LOG_TAG(startuptime) \
  LOG_TAG(state) \
  LOG_TAG(stats) \
  LOG_TAG(streaming) \
  LOG_TAG(stringdedup) \
  LOG_TAG(stringtable) \
  LOG_TAG(subclass) \
  LOG_TAG(survivor) \
  LOG_TAG(suspend) \
  LOG_TAG(sweep) \
  LOG_TAG(symboltable) \
  LOG_TAG(system) \
  LOG_TAG(table) \
  LOG_TAG(task) \
  DEBUG_ONLY(LOG_TAG(test)) \
  LOG_TAG(thread) \
  LOG_TAG(throttle) \
  LOG_TAG(time) \
  LOG_TAG(timer) \
  LOG_TAG(tlab) \
  LOG_TAG(tracking) \
  LOG_TAG(unload) /* Trace unloading of classes */ \
  LOG_TAG(unshareable) \
  LOG_TAG(update) \
  LOG_TAG(valuebasedclasses) \
  LOG_TAG(verification) \
  LOG_TAG(verify) \
  LOG_TAG(vmmutex) \
  LOG_TAG(vmoperation) \
  LOG_TAG(vmthread) \
  LOG_TAG(vtables) \
  LOG_TAG(vtablestubs) \
  LOG_TAG(workgang)

#define PREFIX_LOG_TAG(T) (LogTag::_##T)

// Expand a set of log tags to their prefixed names.
// For error detection purposes, the macro passes one more tag than what is supported.
// If too many tags are given, a static assert in the log class will fail.
#define LOG_TAGS_EXPANDED(T0, T1, T2, T3, T4, T5, ...)  PREFIX_LOG_TAG(T0), PREFIX_LOG_TAG(T1), PREFIX_LOG_TAG(T2), \
                                                        PREFIX_LOG_TAG(T3), PREFIX_LOG_TAG(T4), PREFIX_LOG_TAG(T5)
// The EXPAND_VARARGS macro is required for MSVC, or it will resolve the LOG_TAGS_EXPANDED macro incorrectly.
#define EXPAND_VARARGS(x) x
#define LOG_TAGS(...) EXPAND_VARARGS(LOG_TAGS_EXPANDED(__VA_ARGS__, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG))

// Log tags are used to classify log messages.
// Each log message can be assigned between 1 to LogTag::MaxTags number of tags.
// Specifying multiple tags for a log message means that only outputs configured
// for those exact tags, or a subset of the tags with a wildcard, will see the logging.
// Multiple tags should be comma separated, e.g. log_error(tag1, tag2)("msg").
class LogTag : public AllStatic {
 public:
  // The maximum number of tags that a single log message can have.
  // E.g. there might be hundreds of different tags available,
  // but a specific log message can only be tagged with up to MaxTags of those.
  static const size_t MaxTags = 5;

  enum type {
    __NO_TAG,
#define LOG_TAG(name) _##name,
    LOG_TAG_LIST
#undef LOG_TAG
    Count
  };

  static const char* name(LogTag::type tag) {
    return _name[tag];
  }

  static LogTag::type from_string(const char *str);
  static LogTag::type fuzzy_match(const char *tag);
  static void list_tags(outputStream* out);

 private:
  static const char* const _name[];
};

typedef LogTag::type LogTagType;

#endif // SHARE_LOGGING_LOGTAG_HPP
