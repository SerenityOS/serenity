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
#include "code/relocInfo.hpp"
#include "compiler/compilerDefinitions.hpp"
#include "compiler/compilerDirectives.hpp"
#include "oops/metadata.hpp"
#include "runtime/os.hpp"
#include "interpreter/invocationCounter.hpp"
#include "runtime/arguments.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagConstraintsCompiler.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/powerOfTwo.hpp"

JVMFlag::Error AliasLevelConstraintFunc(intx value, bool verbose) {
  if ((value <= 1) && (Arguments::mode() == Arguments::_comp || Arguments::mode() == Arguments::_mixed)) {
    JVMFlag::printError(verbose,
                        "AliasLevel (" INTX_FORMAT ") is not "
                        "compatible with -Xcomp or -Xmixed\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

/**
 * Validate the minimum number of compiler threads needed to run the JVM.
 */
JVMFlag::Error CICompilerCountConstraintFunc(intx value, bool verbose) {
  int min_number_of_compiler_threads = 0;
#if COMPILER1_OR_COMPILER2
  if (CompilerConfig::is_tiered()) {
    min_number_of_compiler_threads = 2;
  } else if (!CompilerConfig::is_interpreter_only()) {
    min_number_of_compiler_threads = 1;
  }
#else
  if (value > 0) {
    JVMFlag::printError(verbose,
                        "CICompilerCount (" INTX_FORMAT ") cannot be "
                        "greater than 0 because there are no compilers\n", value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
#endif

  if (value < (intx)min_number_of_compiler_threads) {
    JVMFlag::printError(verbose,
                        "CICompilerCount (" INTX_FORMAT ") must be "
                        "at least %d \n",
                        value, min_number_of_compiler_threads);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error AllocatePrefetchDistanceConstraintFunc(intx value, bool verbose) {
  if (value < 0 || value > 512) {
    JVMFlag::printError(verbose,
                        "AllocatePrefetchDistance (" INTX_FORMAT ") must be "
                        "between 0 and %d\n",
                        AllocatePrefetchDistance, 512);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error AllocatePrefetchStepSizeConstraintFunc(intx value, bool verbose) {
  if (AllocatePrefetchStyle == 3) {
    if (value % wordSize != 0) {
      JVMFlag::printError(verbose,
                          "AllocatePrefetchStepSize (" INTX_FORMAT ") must be multiple of %d\n",
                          value, wordSize);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error AllocatePrefetchInstrConstraintFunc(intx value, bool verbose) {
  intx max_value = max_intx;
#if defined(X86)
  max_value = 3;
#endif
  if (value < 0 || value > max_value) {
    JVMFlag::printError(verbose,
                        "AllocatePrefetchInstr (" INTX_FORMAT ") must be "
                        "between 0 and " INTX_FORMAT "\n", value, max_value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error CompileThresholdConstraintFunc(intx value, bool verbose) {
  if (value < 0 || value > INT_MAX >> InvocationCounter::count_shift) {
    JVMFlag::printError(verbose,
                        "CompileThreshold (" INTX_FORMAT ") "
                        "must be between 0 and %d\n",
                        value,
                        INT_MAX >> InvocationCounter::count_shift);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error OnStackReplacePercentageConstraintFunc(intx value, bool verbose) {
  // We depend on CompileThreshold being valid, verify it first.
  if (CompileThresholdConstraintFunc(CompileThreshold, false) == JVMFlag::VIOLATES_CONSTRAINT) {
    JVMFlag::printError(verbose, "OnStackReplacePercentage cannot be validated because CompileThreshold value is invalid\n");
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  int64_t  max_percentage_limit = INT_MAX;
  if (!ProfileInterpreter) {
    max_percentage_limit = (max_percentage_limit>>InvocationCounter::count_shift);
  }
  max_percentage_limit = CompileThreshold == 0  ? max_percentage_limit*100 : max_percentage_limit*100/CompileThreshold;

  if (ProfileInterpreter) {
    if (value < InterpreterProfilePercentage) {
      JVMFlag::printError(verbose,
                          "OnStackReplacePercentage (" INTX_FORMAT ") must be "
                          "larger than InterpreterProfilePercentage (" INTX_FORMAT ")\n",
                          value, InterpreterProfilePercentage);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }

    max_percentage_limit += InterpreterProfilePercentage;
    if (value > max_percentage_limit) {
      JVMFlag::printError(verbose,
                          "OnStackReplacePercentage (" INTX_FORMAT ") must be between 0 and " INT64_FORMAT "\n",
                          value,
                          max_percentage_limit);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  } else {
    if (value < 0) {
      JVMFlag::printError(verbose,
                          "OnStackReplacePercentage (" INTX_FORMAT ") must be "
                          "non-negative\n", value);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }

    if (value > max_percentage_limit) {
      JVMFlag::printError(verbose,
                          "OnStackReplacePercentage (" INTX_FORMAT ") must be between 0 and " INT64_FORMAT "\n",
                          value,
                          max_percentage_limit);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error CodeCacheSegmentSizeConstraintFunc(uintx value, bool verbose) {
  if (CodeCacheSegmentSize < (uintx)CodeEntryAlignment) {
    JVMFlag::printError(verbose,
                        "CodeCacheSegmentSize  (" UINTX_FORMAT ") must be "
                        "larger than or equal to CodeEntryAlignment (" INTX_FORMAT ") "
                        "to align entry points\n",
                        CodeCacheSegmentSize, CodeEntryAlignment);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  if (CodeCacheSegmentSize < sizeof(jdouble)) {
    JVMFlag::printError(verbose,
                        "CodeCacheSegmentSize  (" UINTX_FORMAT ") must be "
                        "at least " SIZE_FORMAT " to align constants\n",
                        CodeCacheSegmentSize, sizeof(jdouble));
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

#ifdef COMPILER2
  if (CodeCacheSegmentSize < (uintx)OptoLoopAlignment) {
    JVMFlag::printError(verbose,
                        "CodeCacheSegmentSize  (" UINTX_FORMAT ") must be "
                        "larger than or equal to OptoLoopAlignment (" INTX_FORMAT ") "
                        "to align inner loops\n",
                        CodeCacheSegmentSize, OptoLoopAlignment);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
#endif

  return JVMFlag::SUCCESS;
}

JVMFlag::Error CodeEntryAlignmentConstraintFunc(intx value, bool verbose) {
  if (!is_power_of_2(value)) {
    JVMFlag::printError(verbose,
                        "CodeEntryAlignment (" INTX_FORMAT ") must be "
                        "a power of two\n", CodeEntryAlignment);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  if (CodeEntryAlignment < 16) {
      JVMFlag::printError(verbose,
                          "CodeEntryAlignment (" INTX_FORMAT ") must be "
                          "greater than or equal to %d\n",
                          CodeEntryAlignment, 16);
      return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error OptoLoopAlignmentConstraintFunc(intx value, bool verbose) {
  if (!is_power_of_2(value)) {
    JVMFlag::printError(verbose,
                        "OptoLoopAlignment (" INTX_FORMAT ") "
                        "must be a power of two\n",
                        value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  // Relevant on ppc, s390. Will be optimized where
  // addr_unit() == 1.
  if (OptoLoopAlignment % relocInfo::addr_unit() != 0) {
    JVMFlag::printError(verbose,
                        "OptoLoopAlignment (" INTX_FORMAT ") must be "
                        "multiple of NOP size (%d)\n",
                        value, relocInfo::addr_unit());
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error ArraycopyDstPrefetchDistanceConstraintFunc(uintx value, bool verbose) {
  if (value >= 4032) {
    JVMFlag::printError(verbose,
                        "ArraycopyDstPrefetchDistance (" UINTX_FORMAT ") must be"
                        "between 0 and 4031\n", value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error AVX3ThresholdConstraintFunc(int value, bool verbose) {
  if (value != 0 && !is_power_of_2(value)) {
    JVMFlag::printError(verbose,
                        "AVX3Threshold ( %d ) must be 0 or "
                        "a power of two value between 0 and MAX_INT\n", value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error ArraycopySrcPrefetchDistanceConstraintFunc(uintx value, bool verbose) {
  if (value >= 4032) {
    JVMFlag::printError(verbose,
                        "ArraycopySrcPrefetchDistance (" UINTX_FORMAT ") must be"
                        "between 0 and 4031\n", value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error TypeProfileLevelConstraintFunc(uintx value, bool verbose) {
  for (int i = 0; i < 3; i++) {
    if (value % 10 > 2) {
      JVMFlag::printError(verbose,
                          "Invalid value (" UINTX_FORMAT ") "
                          "in TypeProfileLevel at position %d\n", value, i);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
    value = value / 10;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error InitArrayShortSizeConstraintFunc(intx value, bool verbose) {
  if (value % BytesPerLong != 0) {
    JVMFlag::printError(verbose,
                        "InitArrayShortSize (" INTX_FORMAT ") must be "
                        "a multiple of %d\n", value, BytesPerLong);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

#ifdef COMPILER2
JVMFlag::Error InteriorEntryAlignmentConstraintFunc(intx value, bool verbose) {
  if (InteriorEntryAlignment > CodeEntryAlignment) {
    JVMFlag::printError(verbose,
                       "InteriorEntryAlignment (" INTX_FORMAT ") must be "
                       "less than or equal to CodeEntryAlignment (" INTX_FORMAT ")\n",
                       InteriorEntryAlignment, CodeEntryAlignment);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  if (!is_power_of_2(value)) {
     JVMFlag::printError(verbose,
                         "InteriorEntryAlignment (" INTX_FORMAT ") must be "
                         "a power of two\n", InteriorEntryAlignment);
     return JVMFlag::VIOLATES_CONSTRAINT;
   }

  int minimum_alignment = 16;
#if defined(X86) && !defined(AMD64)
  minimum_alignment = 4;
#elif defined(S390)
  minimum_alignment = 2;
#endif

  if (InteriorEntryAlignment < minimum_alignment) {
    JVMFlag::printError(verbose,
                        "InteriorEntryAlignment (" INTX_FORMAT ") must be "
                        "greater than or equal to %d\n",
                        InteriorEntryAlignment, minimum_alignment);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error NodeLimitFudgeFactorConstraintFunc(intx value, bool verbose) {
  if (value < MaxNodeLimit * 2 / 100 || value > MaxNodeLimit * 40 / 100) {
    JVMFlag::printError(verbose,
                        "NodeLimitFudgeFactor must be between 2%% and 40%% "
                        "of MaxNodeLimit (" INTX_FORMAT ")\n",
                        MaxNodeLimit);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}
#endif // COMPILER2

JVMFlag::Error RTMTotalCountIncrRateConstraintFunc(int value, bool verbose) {
#if INCLUDE_RTM_OPT
  if (UseRTMLocking && !is_power_of_2(RTMTotalCountIncrRate)) {
    JVMFlag::printError(verbose,
                        "RTMTotalCountIncrRate (%d) must be "
                        "a power of 2, resetting it to 64\n",
                        RTMTotalCountIncrRate);
    FLAG_SET_DEFAULT(RTMTotalCountIncrRate, 64);
  }
#endif

  return JVMFlag::SUCCESS;
}

#ifdef COMPILER2
JVMFlag::Error LoopStripMiningIterConstraintFunc(uintx value, bool verbose) {
  if (UseCountedLoopSafepoints && LoopStripMiningIter == 0) {
    if (!FLAG_IS_DEFAULT(UseCountedLoopSafepoints) || !FLAG_IS_DEFAULT(LoopStripMiningIter)) {
      JVMFlag::printError(verbose,
                          "When counted loop safepoints are enabled, "
                          "LoopStripMiningIter must be at least 1 "
                          "(a safepoint every 1 iteration): setting it to 1\n");
    }
    LoopStripMiningIter = 1;
  } else if (!UseCountedLoopSafepoints && LoopStripMiningIter > 0) {
    if (!FLAG_IS_DEFAULT(UseCountedLoopSafepoints) || !FLAG_IS_DEFAULT(LoopStripMiningIter)) {
      JVMFlag::printError(verbose,
                          "Disabling counted safepoints implies no loop strip mining: "
                          "setting LoopStripMiningIter to 0\n");
    }
    LoopStripMiningIter = 0;
  }

  return JVMFlag::SUCCESS;
}
#endif // COMPILER2

JVMFlag::Error DisableIntrinsicConstraintFunc(ccstrlist value, bool verbose) {
  ControlIntrinsicValidator validator(value, true/*disabled_all*/);
  if (!validator.is_valid()) {
    JVMFlag::printError(verbose,
                        "Unrecognized intrinsic detected in DisableIntrinsic: %s\n",
                        validator.what());
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error ControlIntrinsicConstraintFunc(ccstrlist value, bool verbose) {
  ControlIntrinsicValidator validator(value, false/*disabled_all*/);
  if (!validator.is_valid()) {
    JVMFlag::printError(verbose,
                        "Unrecognized intrinsic detected in ControlIntrinsic: %s\n",
                        validator.what());
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

