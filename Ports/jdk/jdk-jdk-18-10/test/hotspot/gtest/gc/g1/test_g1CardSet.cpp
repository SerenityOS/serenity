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
#include "gc/g1/g1CardSet.inline.hpp"
#include "gc/g1/g1CardSetContainers.hpp"
#include "gc/g1/g1CardSetMemory.hpp"
#include "gc/g1/heapRegionRemSet.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/workgroup.hpp"
#include "logging/log.hpp"
#include "unittest.hpp"
#include "utilities/powerOfTwo.hpp"

class G1CardSetTest : public ::testing::Test {

  class G1CountCardsClosure : public G1CardSet::G1CardSetCardIterator {
  public:
    size_t _num_cards;

    G1CountCardsClosure() : _num_cards(0) { }
    void do_card(uint region_idx, uint card_idx) override {
      _num_cards++;
    }
  };

  static WorkGang* _workers;
  static uint _max_workers;

  static WorkGang* workers() {
    if (_workers == NULL) {
      _max_workers = os::processor_count();
      _workers = new WorkGang("G1CardSetTest Work Gang", _max_workers, false, false);
      _workers->initialize_workers();
      _workers->update_active_workers(_max_workers);
    }
    return _workers;
  }

  // Check whether iteration agrees with the expected number of entries. If the
  // add has been single-threaded, we can also check whether the occupied()
  // (which is an estimate in that case) agrees.
  static void check_iteration(G1CardSet* card_set,
                              const size_t expected,
                              const bool add_was_single_threaded = true);

public:
  G1CardSetTest() { }
  ~G1CardSetTest() { }

  static uint next_random(uint& seed, uint i) {
    // Park–Miller random number generator
    seed = (seed * 279470273u) % 0xfffffffb;
    return (seed % i);
  }

  static void cardset_basic_test();
  static void cardset_mt_test();

  static void add_cards(G1CardSet* card_set, uint cards_per_region, uint* cards, uint num_cards, G1AddCardResult* results);
  static void contains_cards(G1CardSet* card_set, uint cards_per_region, uint* cards, uint num_cards);

  static void translate_cards(uint cards_per_region, uint region_idx, uint* cards, uint num_cards);

  static void iterate_cards(G1CardSet* card_set, G1CardSet::G1CardSetCardIterator* cl);
};

WorkGang* G1CardSetTest::_workers = NULL;
uint G1CardSetTest::_max_workers = 0;

void G1CardSetTest::add_cards(G1CardSet* card_set, uint cards_per_region, uint* cards, uint num_cards, G1AddCardResult* results) {
  for (uint i = 0; i < num_cards; i++) {

    uint region_idx = cards[i] / cards_per_region;
    uint card_idx = cards[i] % cards_per_region;

    G1AddCardResult res = card_set->add_card(region_idx, card_idx);
    if (results != NULL) {
      ASSERT_TRUE(res == results[i]);
    }
  }
}

class G1CheckCardClosure : public G1CardSet::G1CardSetCardIterator {
  G1CardSet* _card_set;

  uint _cards_per_region;
  uint* _cards_to_expect;
  uint _num_cards;

  bool _wrong_region_idx;

public:
  G1CheckCardClosure(G1CardSet* card_set, uint cards_per_region, uint* cards_to_expect, uint num_cards) :
    _card_set(card_set),
    _cards_per_region(cards_per_region),
    _cards_to_expect(cards_to_expect),
    _num_cards(num_cards),
    _wrong_region_idx(false) {
  }

  void do_card(uint region_idx, uint card_idx) override {
    uint card = _cards_per_region * region_idx + card_idx;
    for (uint i = 0; i < _num_cards; i++) {
      if (_cards_to_expect[i] == card) {
        _cards_to_expect[i] = (uint)-1;
      }
    }
  }

  bool all_found() const {
    bool all_good = true;
    for (uint i = 0; i < _num_cards; i++) {
      if (_cards_to_expect[i] != (uint)-1) {
        log_error(gc)("Could not find card %u in region %u",
                      _cards_to_expect[i] % _cards_per_region,
                      _cards_to_expect[i] / _cards_per_region);
        all_good = false;
      }
    }
    return all_good;
  }
};

void G1CardSetTest::contains_cards(G1CardSet* card_set, uint cards_per_region, uint* cards, uint num_cards) {
  for (uint i = 0; i < num_cards; i++) {
    uint region_idx = cards[i] / cards_per_region;
    uint card_idx = cards[i] % cards_per_region;

    ASSERT_TRUE(card_set->contains_card(region_idx, card_idx));
  }

  G1CheckCardClosure cl(card_set, cards_per_region, cards, num_cards);
  card_set->iterate_cards(cl);

  ASSERT_TRUE(cl.all_found());
}

// Offsets the card indexes in the cards array by the region_idx.
void G1CardSetTest::translate_cards(uint cards_per_region, uint region_idx, uint* cards, uint num_cards) {
  for (uint i = 0; i < num_cards; i++) {
    cards[i] = cards_per_region * region_idx + cards[i];
  }
}

class G1CountCardsOccupied : public G1CardSet::G1CardSetPtrIterator {
  size_t _num_occupied;

public:
  G1CountCardsOccupied() : _num_occupied(0) { }

  void do_cardsetptr(uint region_idx, size_t num_occupied, G1CardSet::CardSetPtr card_set) override {
    _num_occupied += num_occupied;
  }

  size_t num_occupied() const { return _num_occupied; }
};

void G1CardSetTest::check_iteration(G1CardSet* card_set, const size_t expected, const bool single_threaded) {

  class CheckIterator : public G1CardSet::G1CardSetCardIterator {
  public:
    G1CardSet* _card_set;
    size_t _num_found;

    CheckIterator(G1CardSet* card_set) : _card_set(card_set), _num_found(0) { }

    void do_card(uint region_idx, uint card_idx) override {
      ASSERT_TRUE(_card_set->contains_card(region_idx, card_idx));
      _num_found++;
    }
  } cl(card_set);

  card_set->iterate_cards(cl);

  ASSERT_TRUE(expected == cl._num_found);
  // We can assert this only if we are single-threaded.
  if (single_threaded) {
    ASSERT_EQ(card_set->occupied(), cl._num_found);
  }
}

void G1CardSetTest::cardset_basic_test() {

  const uint CardsPerRegion = 2048;
  const double FullCardSetThreshold = 0.8;
  const double BitmapCoarsenThreshold = 0.9;

  G1CardSetConfiguration config(log2i_exact(CardsPerRegion), 28, BitmapCoarsenThreshold, 8, FullCardSetThreshold, CardsPerRegion);
  G1CardSetFreePool free_pool(config.num_mem_object_types());
  G1CardSetMemoryManager mm(&config, &free_pool);

  {
    G1CardSet card_set(&config, &mm);

    uint cards1[] = { 1, 2, 3 };
    G1AddCardResult results1[] = { Added, Added, Added };
    translate_cards(CardsPerRegion, 99, cards1, ARRAY_SIZE(cards1));
    add_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1), results1);
    contains_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1));
    ASSERT_TRUE(card_set.occupied() == ARRAY_SIZE(cards1));

    G1CountCardsClosure count_cards;
    card_set.iterate_cards(count_cards);
    ASSERT_TRUE(count_cards._num_cards == ARRAY_SIZE(cards1));

    check_iteration(&card_set, card_set.occupied());

    card_set.clear();
    ASSERT_TRUE(card_set.occupied() == 0);

    check_iteration(&card_set, 0);
  }

  {
    G1CardSet card_set(&config, &mm);

    uint cards1[] = { 0, 2047, 17, 17 };
    G1AddCardResult results1[] = { Added, Added, Added, Found };
    translate_cards(CardsPerRegion, 100, cards1, ARRAY_SIZE(cards1));
    add_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1), results1);
    // -1 because of the duplicate at the end.
    contains_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1) - 1);
    ASSERT_TRUE(card_set.occupied() == ARRAY_SIZE(cards1) - 1);

    G1CountCardsClosure count_cards;
    card_set.iterate_cards(count_cards);
    ASSERT_TRUE(count_cards._num_cards == ARRAY_SIZE(cards1) - 1);

    check_iteration(&card_set, card_set.occupied());

    card_set.clear();
    ASSERT_TRUE(card_set.occupied() == 0);
  }

  {
    G1CardSet card_set(&config, &mm);

    uint cards1[] = { 0, 2047, 17, 18 /* for region 100 */,
                      1,  128, 35, 17 /* for region 990 */
                    };
    translate_cards(CardsPerRegion, 100, &cards1[0], 4);
    translate_cards(CardsPerRegion, 990, &cards1[4], 4);

    add_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1), NULL);
    contains_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1));
    ASSERT_TRUE(card_set.occupied() == ARRAY_SIZE(cards1));

    G1CountCardsClosure count_cards;
    card_set.iterate_cards(count_cards);
    ASSERT_TRUE(count_cards._num_cards == ARRAY_SIZE(cards1));

    check_iteration(&card_set, card_set.occupied());

    card_set.clear();
    ASSERT_TRUE(card_set.occupied() == 0);
  }

  {
    G1CardSet card_set(&config, &mm);

    uint cards1[100];
    for (uint i = 0; i < ARRAY_SIZE(cards1); i++) {
      cards1[i] = i + 3;
      translate_cards(CardsPerRegion, i, &cards1[i], 1);
    }
    add_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1), NULL);
    contains_cards(&card_set, CardsPerRegion, cards1, ARRAY_SIZE(cards1));

    ASSERT_TRUE(card_set.num_containers() == ARRAY_SIZE(cards1));
    ASSERT_TRUE(card_set.occupied() == ARRAY_SIZE(cards1));

    G1CountCardsClosure count_cards;
    card_set.iterate_cards(count_cards);
    ASSERT_TRUE(count_cards._num_cards == ARRAY_SIZE(cards1));

    check_iteration(&card_set, card_set.occupied());

    card_set.clear();
    ASSERT_TRUE(card_set.occupied() == 0);
  }

  {
    G1CardSet card_set(&config, &mm);

    // Generate non-prime numbers from 1 to 1000
    uint count = 0;
    for (uint i = 2; i < 33; i++) {
      if (!card_set.contains_card(100, i)) {
        for (uint j = i * i; j < 1000; j += i) {
          G1AddCardResult res = card_set.add_card(100, j);
          count += (res == Added);
        }
      }
    }

    G1CountCardsOccupied cl;
    card_set.iterate_containers(&cl);

    ASSERT_TRUE(count == card_set.occupied());
    ASSERT_TRUE(card_set.occupied() == cl.num_occupied());

    check_iteration(&card_set, card_set.occupied());

    card_set.clear();
    ASSERT_TRUE(card_set.occupied() == 0);
  }
  { // Test coarsening to full
    G1CardSet card_set(&config, &mm);

    uint count = 0;
    uint i = 10;
    uint bitmap_threshold = config.cards_in_howl_bitmap_threshold();
    for (; i <  bitmap_threshold + 10; i++) {
      G1AddCardResult res = card_set.add_card(99, i);
      ASSERT_TRUE(res == Added);
      count++;
      ASSERT_TRUE(count == card_set.occupied());
    }

    G1AddCardResult res = card_set.add_card(99, config.num_cards_in_howl_bitmap() - 1);
    // Adding above card should have coarsened Bitmap -> Full.
    ASSERT_TRUE(res == Added);
    ASSERT_TRUE(config.num_cards_in_howl_bitmap() == card_set.occupied());

    res = card_set.add_card(99, config.num_cards_in_howl_bitmap() - 2);
    ASSERT_TRUE(res == Found);

    uint threshold = config.cards_in_howl_threshold();
    uint adjusted_threshold = config.cards_in_howl_bitmap_threshold() * config.num_buckets_in_howl();
    i = config.num_cards_in_howl_bitmap();
    count = i;
    for (; i <  threshold; i++) {
      G1AddCardResult res = card_set.add_card(99, i);
      ASSERT_TRUE(res == Added);
      count++;
      ASSERT_TRUE(count == card_set.occupied());
    }

    res = card_set.add_card(99, CardsPerRegion - 1);
    // Adding above card should have coarsened Howl -> Full.
    ASSERT_TRUE(res == Added);
    ASSERT_TRUE(CardsPerRegion == card_set.occupied());

    check_iteration(&card_set, card_set.occupied());

    res = card_set.add_card(99, CardsPerRegion - 2);
    ASSERT_TRUE(res == Found);

    G1CountCardsClosure count_cards;
    card_set.iterate_cards(count_cards);
    ASSERT_TRUE(count_cards._num_cards == config.max_cards_in_region());

    card_set.clear();
    ASSERT_TRUE(card_set.occupied() == 0);
  }
}

class G1CardSetMtTestTask : public AbstractGangTask {
  G1CardSet* _card_set;

  size_t _added;
  size_t _found;

public:
  G1CardSetMtTestTask(G1CardSet* card_set) :
    AbstractGangTask(""),
    _card_set(card_set),
    _added(0),
    _found(0) { }

  void work(uint worker_id) {
    uint seed = worker_id;
    size_t added = 0;
    size_t found = 0;

    for (uint i = 0; i < 100000; i++) {
      uint region = G1CardSetTest::next_random(seed, 1000);
      uint card = G1CardSetTest::next_random(seed, 10000);

      G1AddCardResult res = _card_set->add_card(region, card);

      ASSERT_TRUE(res == Added || res == Found);
      if (res == Added) {
        added++;
      } else if (res == Found) {
        found++;
      }
    }
    Atomic::add(&_added, added);
    Atomic::add(&_found, found);
  }

  size_t added() const { return _added; }
  size_t found() const { return _found; }
};

void G1CardSetTest::cardset_mt_test() {
  const uint CardsPerRegion = 16384;
  const double FullCardSetThreshold = 1.0;
  const uint BitmapCoarsenThreshold = 1.0;

  G1CardSetConfiguration config(log2i_exact(CardsPerRegion), 120, BitmapCoarsenThreshold, 8, FullCardSetThreshold, CardsPerRegion);
  G1CardSetFreePool free_pool(config.num_mem_object_types());
  G1CardSetMemoryManager mm(&config, &free_pool);

  G1CardSet card_set(&config, &mm);

  const uint num_workers = workers()->active_workers();

  G1CardSetMtTestTask cl(&card_set);

  {
    GCTraceTime(Error, gc) x("Cardset test");
    _workers->run_task(&cl, num_workers);
  }

  size_t num_found = 0;
  // Now check the contents of the card set.
  for (uint i = 0; i < num_workers; i++) {
    uint seed = i;

    for (uint j = 0; j < 100000; j++) {
      uint region = G1CardSetTest::next_random(seed, 1000);
      uint card = G1CardSetTest::next_random(seed, 10000);

      bool contains = card_set.contains_card(region, card);
      ASSERT_TRUE(contains);

      num_found += contains;
    }
  }

  ASSERT_TRUE(num_found == cl.added() + cl.found());

  G1CountCardsClosure count_cards;
  card_set.iterate_cards(count_cards);

  check_iteration(&card_set, count_cards._num_cards, false /* add_was_single_threaded */);

  // During coarsening we try to unblock concurrent threads as soon as possible,
  // so we do not add the cards from the smaller CardSetContainer to the larger
  // one immediately, allowing addition by concurrent threads after allocating
  // the space immediately. So the amount of "successfully added" results may be
  // (and in case of many threads typically is) higher than the number of unique
  // cards.
  ASSERT_TRUE(count_cards._num_cards <= cl.added());
}

TEST_VM(G1CardSetTest, basic_cardset_test) {
  G1CardSetTest::cardset_basic_test();
}

TEST_VM(G1CardSetTest, mt_cardset_test) {
  G1CardSetTest::cardset_mt_test();
}
