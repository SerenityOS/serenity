/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Generator.h>
#include <LibTest/AsyncTestCase.h>

namespace {

Generator<int, Empty> generate_sync(Vector<int>& order)
{
    ScopeGuard guard = [&] {
        order.append(7);
    };

    order.append(2);
    co_yield 1;
    order.append(4);
    co_yield 2;
    order.append(6);
    co_return {};
}

}

ASYNC_TEST_CASE(sync_order)
{
    Vector<int> order;

    auto gen = generate_sync(order);
    EXPECT(!gen.is_done());

    order.append(1);

    auto result1 = gen.next();
    order.append(3);
    EXPECT(result1.await_ready());
    EXPECT_EQ(result1.await_resume(), 1);

    auto result2 = gen.next();
    order.append(5);
    EXPECT(result2.await_ready());
    EXPECT_EQ(result2.await_resume(), 2);

    auto end = gen.next();
    order.append(8);
    EXPECT(end.await_ready());
    EXPECT_EQ(end.await_resume(), Empty {});

    EXPECT_EQ(order, (Vector<int> { 1, 2, 3, 4, 5, 6, 7, 8 }));
    co_return;
}
