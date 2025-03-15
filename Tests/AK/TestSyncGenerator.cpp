/*
 * Copyright (c) 2025, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SyncGenerator.h>
#include <AK/Vector.h>
#include <LibTest/TestCase.h>

namespace {

SyncGenerator<int> generate(Vector<int>& order)
{
    ScopeGuard guard = [&] {
        order.append(7);
    };
    order.append(2);
    co_yield 1;
    order.append(4);
    co_yield 2;
    order.append(6);
}

}

TEST_CASE(simple)
{
    Vector<int> order;

    auto gen = generate(order);
    EXPECT(!gen.is_done());

    order.append(1);

    auto result1 = gen.next();
    order.append(3);
    EXPECT(!gen.is_done());
    EXPECT_EQ(result1, 1);

    auto result2 = gen.next();
    order.append(5);
    EXPECT(!gen.is_done());
    EXPECT_EQ(result2, 2);

    auto end = gen.next();
    order.append(8);
    EXPECT(gen.is_done());
    EXPECT(!end.has_value());

    EXPECT_EQ(order, (Vector<int> { 1, 2, 3, 4, 5, 6, 7, 8 }));
}

TEST_CASE(move)
{
    Vector<int> order;

    auto gen = generate(order);
    EXPECT(!gen.is_done());

    EXPECT_EQ(gen.next(), 1);

    auto moved_gen = move(gen);

    EXPECT_EQ(moved_gen.next(), 2);

    EXPECT(!moved_gen.next().has_value());

    EXPECT(moved_gen.is_done());
}

namespace {

class MoveCounter {
    AK_MAKE_NONCOPYABLE(MoveCounter);

public:
    MoveCounter()
        : m_move_count(0)
    {
    }

    MoveCounter(MoveCounter&& other)
        : m_move_count(exchange(other.m_move_count, 0) + 1)
    {
    }

    MoveCounter& operator=(MoveCounter&& other)
    {
        if (this != &other) {
            this->~MoveCounter();
            new (this) MoveCounter(move(other));
        }
        return *this;
    }

    int move_count() const { return m_move_count; }

private:
    int m_move_count;
};

SyncGenerator<MoveCounter> generate2()
{
    co_yield MoveCounter {};
    MoveCounter counter;
    co_yield move(counter);
}

}

TEST_CASE(move_count)
{
    auto gen = generate2();
    auto result = gen.next();
    EXPECT_EQ(result->move_count(), 2);
    EXPECT_EQ(gen.next()->move_count(), 2);
    EXPECT(!gen.next().has_value());
}
