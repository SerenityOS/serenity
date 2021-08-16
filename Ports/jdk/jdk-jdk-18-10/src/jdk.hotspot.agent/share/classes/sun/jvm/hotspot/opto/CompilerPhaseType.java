/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.opto;

//These definitions should be kept in sync with the definitions in the HotSpot code.

public enum CompilerPhaseType {
  PHASE_BEFORE_STRINGOPTS ("Before StringOpts"),
  PHASE_AFTER_STRINGOPTS ("After StringOpts"),
  PHASE_BEFORE_REMOVEUSELESS ("Before RemoveUseless"),
  PHASE_AFTER_PARSING ("After Parsing"),
  PHASE_ITER_GVN1 ("Iter GVN 1"),
  PHASE_PHASEIDEAL_BEFORE_EA ("PhaseIdealLoop before EA"),
  PHASE_ITER_GVN_AFTER_EA ("Iter GVN after EA"),
  PHASE_ITER_GVN_AFTER_ELIMINATION ("Iter GVN after eliminating allocations and locks"),
  PHASE_PHASEIDEALLOOP1 ("PhaseIdealLoop 1"),
  PHASE_PHASEIDEALLOOP2 ("PhaseIdealLoop 2"),
  PHASE_PHASEIDEALLOOP3 ("PhaseIdealLoop 3"),
  PHASE_CCP1 ("PhaseCCP 1"),
  PHASE_ITER_GVN2 ("Iter GVN 2"),
  PHASE_PHASEIDEALLOOP_ITERATIONS ("PhaseIdealLoop iterations"),
  PHASE_OPTIMIZE_FINISHED ("Optimize finished"),
  PHASE_GLOBAL_CODE_MOTION ("Global code motion"),
  PHASE_FINAL_CODE ("Final Code"),
  PHASE_AFTER_EA ("After Escape Analysis"),
  PHASE_BEFORE_CLOOPS ("Before CountedLoop"),
  PHASE_AFTER_CLOOPS ("After CountedLoop"),
  PHASE_BEFORE_BEAUTIFY_LOOPS ("Before beautify loops"),
  PHASE_AFTER_BEAUTIFY_LOOPS ("After beautify loops"),
  PHASE_BEFORE_MATCHING ("Before Matching"),
  PHASE_INCREMENTAL_INLINE ("Incremental Inline"),
  PHASE_INCREMENTAL_BOXING_INLINE ("Incremental Boxing Inline"),
  PHASE_END ("End"),
  PHASE_FAILURE ("Failure"),
  PHASE_NUM_TYPES ("Number of Phase Types");

  private final String value;

  CompilerPhaseType(String val) {
    this.value = val;
  }
  public String value() {
    return value;
  }
}
