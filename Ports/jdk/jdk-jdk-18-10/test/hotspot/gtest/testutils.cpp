/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 SAP SE. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "runtime/os.hpp"
#include "utilities/align.hpp"
#include "utilities/ostream.hpp"

#include "testutils.hpp"
#include "unittest.hpp"

#include <string.h>

// Note: these could be made more suitable for covering large ranges (e.g. just mark one byte per page).

void GtestUtils::mark_range_with(void* p, size_t s, uint8_t mark) {
  if (p != NULL && s > 0) {
    ::memset(p, mark, s);
  }
}

bool GtestUtils::check_range(const void* p, size_t s, uint8_t expected) {
  if (p == NULL || s == 0) {
    return true;
  }

  const char* first_wrong = NULL;
  char* p2 = (char*)p;
  const char* const end = p2 + s;
  while (p2 < end) {
    if (*p2 != (char)expected) {
      first_wrong = p2;
      break;
    }
    p2 ++;
  }

  if (first_wrong != NULL) {
    tty->print_cr("wrong pattern around " PTR_FORMAT, p2i(first_wrong));
    // Note: We deliberately print the surroundings too without bounds check. Might be interesting,
    // and os::print_hex_dump uses SafeFetch, so this is fine without bounds checks.
    os::print_hex_dump(tty, (address)(align_down(p2, 0x10) - 0x10),
                            (address)(align_up(end, 0x10) + 0x10), 1);
  }

  return first_wrong == NULL;
}
