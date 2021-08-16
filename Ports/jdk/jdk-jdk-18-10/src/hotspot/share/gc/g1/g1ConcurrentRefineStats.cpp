/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1ConcurrentRefineStats.hpp"

G1ConcurrentRefineStats::G1ConcurrentRefineStats() :
  _refinement_time(),
  _refined_cards(0),
  _precleaned_cards(0),
  _dirtied_cards(0)
{}

G1ConcurrentRefineStats&
G1ConcurrentRefineStats::operator+=(const G1ConcurrentRefineStats& other) {
  _refinement_time += other._refinement_time;
  _refined_cards += other._refined_cards;
  _precleaned_cards += other._precleaned_cards;
  _dirtied_cards += other._dirtied_cards;
  return *this;
}

template<typename T>
static T clipped_sub(T x, T y) {
  return (x < y) ? T() : (x - y);
}

G1ConcurrentRefineStats&
G1ConcurrentRefineStats::operator-=(const G1ConcurrentRefineStats& other) {
  _refinement_time = clipped_sub(_refinement_time, other._refinement_time);
  _refined_cards = clipped_sub(_refined_cards, other._refined_cards);
  _precleaned_cards = clipped_sub(_precleaned_cards, other._precleaned_cards);
  _dirtied_cards = clipped_sub(_dirtied_cards, other._dirtied_cards);
  return *this;
}

void G1ConcurrentRefineStats::reset() {
  *this = G1ConcurrentRefineStats();
}
