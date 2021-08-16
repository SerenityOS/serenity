/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_METASPACECOMMON_HPP
#define SHARE_MEMORY_METASPACE_METASPACECOMMON_HPP

#include "runtime/globals.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

// Metaspace allocation alignment:

// 1) Metaspace allocations have to be aligned such that 64bit values are aligned
//  correctly.
//
// 2) Klass* structures allocated from Metaspace have to be aligned to KlassAlignmentInBytes.
//
// At the moment LogKlassAlignmentInBytes is 3, so KlassAlignmentInBytes == 8,
//  so (1) and (2) can both be fulfilled with an alignment of 8. Should we increase
//  KlassAlignmentInBytes at any time this will increase the necessary alignment as well. In
//  that case we may think about introducing a separate alignment just for the class space
//  since that alignment would only be needed for Klass structures.

static const size_t AllocationAlignmentByteSize = 8;
STATIC_ASSERT(AllocationAlignmentByteSize == (size_t)KlassAlignmentInBytes);

static const size_t AllocationAlignmentWordSize = AllocationAlignmentByteSize / BytesPerWord;

// Returns the raw word size allocated for a given net allocation
size_t get_raw_word_size_for_requested_word_size(size_t word_size);

// Utility functions

// Print a size, in words, scaled.
void print_scaled_words(outputStream* st, size_t word_size, size_t scale = 0, int width = -1);

// Convenience helper: prints a size value and a percentage.
void print_scaled_words_and_percentage(outputStream* st, size_t word_size, size_t compare_word_size, size_t scale = 0, int width = -1);

// Print a human readable size.
// byte_size: size, in bytes, to be printed.
// scale: one of 1 (byte-wise printing), sizeof(word) (word-size printing), K, M, G (scaled by KB, MB, GB respectively,
//         or 0, which means the best scale is chosen dynamically.
// width: printing width.
void print_human_readable_size(outputStream* st, size_t byte_size, size_t scale = 0, int width = -1);

// Prints a percentage value. Values smaller than 1% but not 0 are displayed as "<1%", values
// larger than 99% but not 100% are displayed as ">100%".
void print_percentage(outputStream* st, size_t total, size_t part);

#ifdef ASSERT
#define assert_is_aligned(value, alignment)                  \
  assert(is_aligned((value), (alignment)),                   \
         SIZE_FORMAT_HEX " is not aligned to "               \
         SIZE_FORMAT_HEX, (size_t)(uintptr_t)value, (size_t)(alignment))
#else
#define assert_is_aligned(value, alignment)
#endif

// Pretty printing helpers
const char* classes_plural(uintx num);
const char* loaders_plural(uintx num);
void print_number_of_classes(outputStream* out, uintx classes, uintx classes_shared);

// Since Metaspace verifications are expensive, we want to do them at a reduced rate,
// but not completely avoiding them.
// For that we introduce the macros SOMETIMES() and ASSERT_SOMETIMES() which will
// execute code or assert at intervals controlled via VerifyMetaspaceInterval.
#ifdef ASSERT

#define EVERY_NTH(n)          \
{ static int counter_ = 0;    \
  if (n > 0) {                \
    counter_++;              \
    if (counter_ >= n) {      \
      counter_ = 0;           \

#define END_EVERY_NTH         } } }

#define SOMETIMES(code) \
    EVERY_NTH(VerifyMetaspaceInterval) \
    { code } \
    END_EVERY_NTH

#define ASSERT_SOMETIMES(condition, ...) \
    EVERY_NTH(VerifyMetaspaceInterval) \
    assert( (condition), __VA_ARGS__); \
    END_EVERY_NTH

#else

#define SOMETIMES(code)
#define ASSERT_SOMETIMES(condition, ...)

#endif // ASSERT

///////// Logging //////////////

// What we log at which levels:

// "info" : metaspace failed allocation, commit failure, reserve failure, metaspace oom, metaspace gc threshold changed, Arena created, destroyed, metaspace purged

// "debug" : "info" + vslist extended, memory committed/uncommitted, chunk created/split/merged/enlarged, chunk returned

// "trace" : "debug" + every single allocation and deallocation, internals

#define HAVE_UL

#ifdef HAVE_UL
#define UL(level, message)        log_##level(metaspace)(LOGFMT ": " message, LOGFMT_ARGS);
#define UL2(level, message, ...)  log_##level(metaspace)(LOGFMT ": " message, LOGFMT_ARGS, __VA_ARGS__);
#else
#define UL(level, ...)
#define UL2(level, ...)
#endif

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_METASPACECOMMON_HPP
