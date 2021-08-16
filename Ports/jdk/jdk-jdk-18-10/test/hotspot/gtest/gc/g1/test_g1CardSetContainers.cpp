/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CardSetContainers.inline.hpp"
#include "gc/g1/heapRegionBounds.inline.hpp"
#include "gc/shared/cardTable.hpp"
#include "memory/allocation.inline.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"
#include "unittest.hpp"

class G1CardSetContainersTest : public ::testing::Test {
public:
  G1CardSetContainersTest() { }
  ~G1CardSetContainersTest() { }

  static uint cards_per_inlineptr_set(uint bits_per_card) {
    return G1CardSetInlinePtr::max_cards_in_inline_ptr(bits_per_card);
  }

  static void cardset_inlineptr_test(uint bits_per_card);
  static void cardset_array_test(uint cards_per_array);
  static void cardset_bitmap_test(uint threshold, uint size_in_bits);
};

class G1FindCardsInRange : public StackObj {
  uint _num_cards;
  uint _range_min;
  bool* _cards_found;
public:
  G1FindCardsInRange(uint range_min, uint range_max) :
    _num_cards(range_max - range_min + 1),
    _range_min(range_min),
    _cards_found(NEW_C_HEAP_ARRAY(bool, _num_cards, mtGC)) {
    for (uint i = 0; i < _num_cards; i++) {
      _cards_found[i] = false;
    }
  }

  void verify_all_found() {
    verify_part_found(_num_cards);
  }

  void verify_part_found(uint num) {
    for (uint i = 0; i < num; i++) {
      ASSERT_TRUE(_cards_found[i]);
    }
  }

  ~G1FindCardsInRange() {
    FREE_C_HEAP_ARRAY(mtGC, _cards_found);
  }
  void operator()(uint card) {
    ASSERT_TRUE((card - _range_min) < _num_cards);
    ASSERT_FALSE(_cards_found[card - _range_min]); // Must not have been found yet.
    _cards_found[card - _range_min] = true;
  }
};

void G1CardSetContainersTest::cardset_inlineptr_test(uint bits_per_card) {
  const uint CardsPerSet = cards_per_inlineptr_set(bits_per_card);

  G1AddCardResult res;

  G1CardSet::CardSetPtr value = G1CardSetInlinePtr();

  for (uint i = 0; i < CardsPerSet; i++) {
    {
      G1CardSetInlinePtr cards(&value, value);
      res = cards.add(i + 1, bits_per_card, CardsPerSet);
      ASSERT_TRUE(res == Added);
    }
    {
      G1CardSetInlinePtr cards(&value, value);
      ASSERT_TRUE(cards.contains(i + 1, bits_per_card));
    }
  }

  for (uint i = 0; i < CardsPerSet; i++) {
    G1CardSetInlinePtr cards(value);
    ASSERT_TRUE(cards.contains(i + 1, bits_per_card));
  }

  // Try to add again, should all return that the card had been added.
  for (uint i = 0; i < CardsPerSet; i++) {
    G1CardSetInlinePtr cards(&value, value);
    res = cards.add(i + 1, bits_per_card, CardsPerSet);
    ASSERT_TRUE(res == Found);
  }

  // Should be no more space in set.
  {
    G1CardSetInlinePtr cards(&value, value);
    res = cards.add(CardsPerSet + 1, bits_per_card, CardsPerSet);
    ASSERT_TRUE(res == Overflow);
  }

  // Cards should still be in the set.
  for (uint i = 0; i < CardsPerSet; i++) {
    G1CardSetInlinePtr cards(value);
    ASSERT_TRUE(cards.contains(i + 1, bits_per_card));
  }

  // Boundary cards should not be in the set.
  {
    G1CardSetInlinePtr cards(value);
    ASSERT_TRUE(!cards.contains(0, bits_per_card));
    ASSERT_TRUE(!cards.contains(CardsPerSet + 1, bits_per_card));
  }

  // Verify iteration finds all cards too and only those.
  {
    G1FindCardsInRange found(1, CardsPerSet);
    G1CardSetInlinePtr cards(value);
    cards.iterate(found, bits_per_card);
    found.verify_all_found();
  }
}

void G1CardSetContainersTest::cardset_array_test(uint cards_per_array) {
  uint8_t* cardset_data = NEW_C_HEAP_ARRAY(uint8_t, G1CardSetArray::size_in_bytes(cards_per_array), mtGC);
  G1CardSetArray* cards = new (cardset_data) G1CardSetArray(1, cards_per_array);

  ASSERT_TRUE(cards->contains(1)); // Added during initialization
  ASSERT_TRUE(cards->num_entries() == 1); // Check it's the only one.

  G1AddCardResult res;

  // Add some elements
  for (uint i = 1; i < cards_per_array; i++) {
    res = cards->add(i + 1);
    ASSERT_TRUE(res == Added);
  }

  // Check they are in the container.
  for (uint i = 0; i < cards_per_array; i++) {
    ASSERT_TRUE(cards->contains(i + 1));
  }

  // Try to add again, should all return that the card had been added.
  for (uint i = 0; i < cards_per_array; i++) {
    res = cards->add(i + 1);
    ASSERT_TRUE(res == Found);
  }

  // Should be no more space in set.
  {
    res = cards->add(cards_per_array + 1);
    ASSERT_TRUE(res == Overflow);
  }

  // Cards should still be in the set.
  for (uint i = 0; i < cards_per_array; i++) {
    ASSERT_TRUE(cards->contains(i + 1));
  }

  ASSERT_TRUE(!cards->contains(0));
  ASSERT_TRUE(!cards->contains(cards_per_array + 1));

  // Verify iteration finds all cards too.
  {
    G1FindCardsInRange found(1, cards_per_array);
    cards->iterate(found);
    found.verify_all_found();
  }

  FREE_C_HEAP_ARRAY(mtGC, cardset_data);
}

void G1CardSetContainersTest::cardset_bitmap_test(uint threshold, uint size_in_bits) {
  uint8_t* cardset_data = NEW_C_HEAP_ARRAY(uint8_t, G1CardSetBitMap::size_in_bytes(size_in_bits), mtGC);
  G1CardSetBitMap* cards = new (cardset_data) G1CardSetBitMap(1, size_in_bits);

  ASSERT_TRUE(cards->contains(1, size_in_bits)); // Added during initialization
  ASSERT_TRUE(cards->num_bits_set() == 1); // Should be the only one.

  G1AddCardResult res;

  for (uint i = 1; i < threshold; i++) {
    res = cards->add(i + 1, threshold, size_in_bits);
    ASSERT_TRUE(res == Added);
  }

  for (uint i = 0; i < threshold; i++) {
    ASSERT_TRUE(cards->contains(i + 1, size_in_bits));
  }

  // Try to add again, should all return that the card had been added.
  for (uint i = 0; i < threshold; i++) {
    res = cards->add(i + 1, threshold, size_in_bits);
    ASSERT_TRUE(res == Found);
  }

  // Should be no more space in set.
  {
    res = cards->add(threshold + 1, threshold, size_in_bits);
    ASSERT_TRUE(res == Overflow);
  }

  // Cards should still be in the set.
  for (uint i = 0; i < threshold; i++) {
    ASSERT_TRUE(cards->contains(i + 1, size_in_bits));
  }

  ASSERT_TRUE(!cards->contains(0, size_in_bits));

  // Verify iteration finds all cards too.
  {
    G1FindCardsInRange found(1, threshold + 1);
    cards->iterate(found, size_in_bits, 0);
    found.verify_part_found(threshold);
  }

  FREE_C_HEAP_ARRAY(mtGC, cardset_data);
}

TEST_VM_F(G1CardSetContainersTest, basic_cardset_inptr_test) {
  uint const min = (uint)log2i(HeapRegionBounds::min_size());
  uint const max = (uint)log2i(HeapRegionBounds::max_size());

  for (uint i = min; i <= max; i++) {
    G1CardSetContainersTest::cardset_inlineptr_test(i - CardTable::card_shift);
  }
}

TEST_VM_F(G1CardSetContainersTest, basic_cardset_array_test) {
  uint array_sizes[] = { 5, 9, 63, 77, 127 };

  for (uint i = 0; i < ARRAY_SIZE(array_sizes); i++) {
    size_t const max_cards_in_set = ARRAY_SIZE(array_sizes);
    G1CardSetContainersTest::cardset_array_test(max_cards_in_set);
  }
}

TEST_VM_F(G1CardSetContainersTest, basic_cardset_bitmap_test) {
  uint bit_sizes[] = { 64, 2048 };
  uint threshold_sizes[] = { 17, 330 };

  for (uint i = 0; i < ARRAY_SIZE(bit_sizes); i++) {
    G1CardSetContainersTest::cardset_bitmap_test(threshold_sizes[i], bit_sizes[i]);
  }
}
