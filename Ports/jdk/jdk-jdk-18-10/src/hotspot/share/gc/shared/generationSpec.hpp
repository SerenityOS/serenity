/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GENERATIONSPEC_HPP
#define SHARE_GC_SHARED_GENERATIONSPEC_HPP

#include "gc/shared/generation.hpp"
#include "utilities/align.hpp"

// The specification of a generation.  This class also encapsulates
// some generation-specific behavior.  This is done here rather than as a
// virtual function of Generation because these methods are needed in
// initialization of the Generations.
class GenerationSpec : public CHeapObj<mtGC> {
  friend class VMStructs;
private:
  Generation::Name _name;
  size_t           _init_size;
  size_t           _min_size;
  size_t           _max_size;

public:
  GenerationSpec(Generation::Name name, size_t init_size, size_t max_size, size_t alignment) :
    _name(name),
    _init_size(align_up(init_size, alignment)),
    _min_size(_init_size),
    _max_size(align_up(max_size, alignment))
  { }

  Generation* init(ReservedSpace rs, CardTableRS* remset);

  Generation::Name name() const { return _name; }
  size_t init_size()      const { return _init_size; }
  size_t min_size()       const { return _min_size; }
  size_t max_size()       const { return _max_size; }
};

typedef GenerationSpec* GenerationSpecPtr;

#endif // SHARE_GC_SHARED_GENERATIONSPEC_HPP
