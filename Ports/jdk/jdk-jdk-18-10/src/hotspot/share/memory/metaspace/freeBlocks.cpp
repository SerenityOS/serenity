/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "memory/metaspace/freeBlocks.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

void FreeBlocks::add_block(MetaWord* p, size_t word_size) {
  assert(word_size >= MinWordSize, "sanity (" SIZE_FORMAT ")", word_size);
  if (word_size > MaxSmallBlocksWordSize) {
    _tree.add_block(p, word_size);
  } else {
    _small_blocks.add_block(p, word_size);
  }
}

MetaWord* FreeBlocks::remove_block(size_t requested_word_size) {
  assert(requested_word_size >= MinWordSize,
      "requested_word_size too small (" SIZE_FORMAT ")", requested_word_size);
  size_t real_size = 0;
  MetaWord* p = NULL;
  if (requested_word_size > MaxSmallBlocksWordSize) {
    p = _tree.remove_block(requested_word_size, &real_size);
  } else {
    p = _small_blocks.remove_block(requested_word_size, &real_size);
  }
  if (p != NULL) {
    // Blocks which are larger than a certain threshold are split and
    //  the remainder is handed back to the manager.
    const size_t waste = real_size - requested_word_size;
    if (waste > MinWordSize) {
      add_block(p + requested_word_size, waste);
    }
  }
  return p;
}

} // namespace metaspace

