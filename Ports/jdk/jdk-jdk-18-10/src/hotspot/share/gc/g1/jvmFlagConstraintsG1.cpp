/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/heapRegionBounds.inline.hpp"
#include "gc/g1/jvmFlagConstraintsG1.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/globalDefinitions.hpp"

JVMFlag::Error G1RemSetArrayOfCardsEntriesConstraintFunc(uint value, bool verbose) {
  if (!UseG1GC) return JVMFlag::SUCCESS;

  // Default value of G1RemSetArrayOfCardsEntries=0 means will be set ergonomically.
  // Minimum value is 1.
  if (FLAG_IS_CMDLINE(G1RemSetArrayOfCardsEntries) && (value < 1)) {
    JVMFlag::printError(verbose,
                        "G1RemSetArrayOfCardsEntries (%u) must be "
                        "greater than or equal to 1.\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error G1RemSetHowlNumBucketsConstraintFunc(uint value, bool verbose) {
  if (!UseG1GC) return JVMFlag::SUCCESS;

  if (!FLAG_IS_CMDLINE(G1RemSetHowlNumBuckets)) {
    return JVMFlag::SUCCESS;
  }
  if (value == 0 || !is_power_of_2(G1RemSetHowlNumBuckets)) {
    JVMFlag::printError(verbose,
                        "G1RemSetHowlNumBuckets (%u) must be a power of two "
                        "and greater than or equal to 1.\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error G1RemSetHowlMaxNumBucketsConstraintFunc(uint value, bool verbose) {
  if (!UseG1GC) return JVMFlag::SUCCESS;

  if (!FLAG_IS_CMDLINE(G1RemSetHowlMaxNumBuckets)) {
    return JVMFlag::SUCCESS;
  }
  if (!is_power_of_2(G1RemSetHowlMaxNumBuckets)) {
    JVMFlag::printError(verbose,
                        "G1RemSetMaxHowlNumBuckets (%u) must be a power of two.\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error G1HeapRegionSizeConstraintFunc(size_t value, bool verbose) {
  if (!UseG1GC) return JVMFlag::SUCCESS;

  // Default value of G1HeapRegionSize=0 means will be set ergonomically.
  if (FLAG_IS_CMDLINE(G1HeapRegionSize) && (value < HeapRegionBounds::min_size())) {
    JVMFlag::printError(verbose,
                        "G1HeapRegionSize (" SIZE_FORMAT ") must be "
                        "greater than or equal to ergonomic heap region minimum size\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error G1NewSizePercentConstraintFunc(uintx value, bool verbose) {
  if (!UseG1GC) return JVMFlag::SUCCESS;

  if (value > G1MaxNewSizePercent) {
    JVMFlag::printError(verbose,
                        "G1NewSizePercent (" UINTX_FORMAT ") must be "
                        "less than or equal to G1MaxNewSizePercent (" UINTX_FORMAT ")\n",
                        value, G1MaxNewSizePercent);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error G1MaxNewSizePercentConstraintFunc(uintx value, bool verbose) {
  if (!UseG1GC) return JVMFlag::SUCCESS;

  if (value < G1NewSizePercent) {
    JVMFlag::printError(verbose,
                        "G1MaxNewSizePercent (" UINTX_FORMAT ") must be "
                        "greater than or equal to G1NewSizePercent (" UINTX_FORMAT ")\n",
                        value, G1NewSizePercent);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error MaxGCPauseMillisConstraintFuncG1(uintx value, bool verbose) {
  if (UseG1GC && FLAG_IS_CMDLINE(MaxGCPauseMillis) && (value >= GCPauseIntervalMillis)) {
    JVMFlag::printError(verbose,
                        "MaxGCPauseMillis (" UINTX_FORMAT ") must be "
                        "less than GCPauseIntervalMillis (" UINTX_FORMAT ")\n",
                        value, GCPauseIntervalMillis);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error GCPauseIntervalMillisConstraintFuncG1(uintx value, bool verbose) {
  if (UseG1GC) {
    if (FLAG_IS_CMDLINE(GCPauseIntervalMillis)) {
      if (value < 1) {
        JVMFlag::printError(verbose,
                            "GCPauseIntervalMillis (" UINTX_FORMAT ") must be "
                            "greater than or equal to 1\n",
                            value);
        return JVMFlag::VIOLATES_CONSTRAINT;
      }

      if (FLAG_IS_DEFAULT(MaxGCPauseMillis)) {
        JVMFlag::printError(verbose,
                            "GCPauseIntervalMillis cannot be set "
                            "without setting MaxGCPauseMillis\n");
        return JVMFlag::VIOLATES_CONSTRAINT;
      }

      if (value <= MaxGCPauseMillis) {
        JVMFlag::printError(verbose,
                            "GCPauseIntervalMillis (" UINTX_FORMAT ") must be "
                            "greater than MaxGCPauseMillis (" UINTX_FORMAT ")\n",
                            value, MaxGCPauseMillis);
        return JVMFlag::VIOLATES_CONSTRAINT;
      }
    }
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error NewSizeConstraintFuncG1(size_t value, bool verbose) {
#ifdef _LP64
  // Overflow would happen for uint type variable of YoungGenSizer::_min_desired_young_length
  // when the value to be assigned exceeds uint range.
  // i.e. result of '(uint)(NewSize / region size(1~32MB))'
  // So maximum of NewSize should be 'max_juint * 1M'
  if (UseG1GC && (value > (max_juint * 1 * M))) {
    JVMFlag::printError(verbose,
                        "NewSize (" SIZE_FORMAT ") must be less than ergonomic maximum value\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
#endif // _LP64
  return JVMFlag::SUCCESS;
}

size_t MaxSizeForHeapAlignmentG1() {
  return HeapRegionBounds::max_size();
}
