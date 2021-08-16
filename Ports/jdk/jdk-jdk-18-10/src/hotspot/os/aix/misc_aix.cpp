/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
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

#include "misc_aix.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/align.hpp"

#include <pthread.h>
#include <unistd.h>
#include <errno.h>

void MiscUtils::init_critsect(MiscUtils::critsect_t* cs) {
  const int rc = pthread_mutex_init(cs, NULL);
  assert0(rc == 0);
}

void MiscUtils::free_critsect(MiscUtils::critsect_t* cs) {
  const int rc = pthread_mutex_destroy(cs);
  assert0(rc == 0);
}

void MiscUtils::enter_critsect(MiscUtils::critsect_t* cs) {
  const int rc = pthread_mutex_lock(cs);
  assert0(rc == 0);
}

void MiscUtils::leave_critsect(MiscUtils::critsect_t* cs) {
  const int rc = pthread_mutex_unlock(cs);
  assert0(rc == 0);
}
