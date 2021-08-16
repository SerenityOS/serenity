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
#include "memory/metaspace/metachunkList.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

namespace metaspace {

#ifdef ASSERT

void MetachunkList::verify_does_not_contain(const Metachunk* c) const {
  SOMETIMES(assert(contains(c) == false, "List contains this chunk.");)
}

bool MetachunkList::contains(const Metachunk* c) const {
  for (Metachunk* c2 = _first; c2 != NULL; c2 = c2->next()) {
    if (c == c2) {
      return true;
    }
  }
  return false;
}

void MetachunkList::verify() const {
  int num = 0;
  const Metachunk* last_c = NULL;
  for (const Metachunk* c = _first; c != NULL; c = c->next()) {
    num++;
    assert(c->prev() != c && c->next() != c, "circularity");
    assert(c->prev() == last_c,
           "Broken link to predecessor. Chunk " METACHUNK_FULL_FORMAT ".",
           METACHUNK_FULL_FORMAT_ARGS(c));
    c->verify();
    last_c = c;
  }
  _num_chunks.check(num);
}

#endif // ASSERT

size_t MetachunkList::calc_committed_word_size() const {
  if (_first != NULL && _first->is_dead()) {
    // list used for chunk header pool; dead chunks have no size.
    return 0;
  }
  size_t s = 0;
  for (Metachunk* c = _first; c != NULL; c = c->next()) {
    assert(c->is_dead() == false, "Sanity");
    s += c->committed_words();
  }
  return s;
}

size_t MetachunkList::calc_word_size() const {
  if (_first != NULL && _first->is_dead()) {
    // list used for chunk header pool; dead chunks have no size.
    return 0;
  }
  size_t s = 0;
  for (Metachunk* c = _first; c != NULL; c = c->next()) {
    assert(c->is_dead() == false, "Sanity");
    s += c->committed_words();
  }
  return s;
}

void MetachunkList::print_on(outputStream* st) const {
  if (_num_chunks.get() > 0) {
    for (const Metachunk* c = _first; c != NULL; c = c->next()) {
      st->print(" - <");
      c->print_on(st);
      st->print(">");
    }
    st->print(" - total : %d chunks.", _num_chunks.get());
  } else {
    st->print("empty");
  }
}

} // namespace metaspace

