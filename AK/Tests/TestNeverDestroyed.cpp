/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

#include <AK/NeverDestroyed.h>
#include <AK/StdLibExtras.h>

struct Counter {
    Counter() = default;

    ~Counter() { ++num_destroys; }

    Counter(const Counter&)
    {
        ++num_copies;
    }

    Counter(Counter&&) { ++num_moves; }

    int num_copies {};
    int num_moves {};
    int num_destroys {};
};

TEST_CASE(should_construct_by_copy)
{
    Counter c {};
    AK::NeverDestroyed<Counter> n { c };

    EXPECT_EQ(1, n->num_copies);
    EXPECT_EQ(0, n->num_moves);
}

TEST_CASE(should_construct_by_move)
{
    Counter c {};
    AK::NeverDestroyed<Counter> n { move(c) };

    EXPECT_EQ(0, n->num_copies);
    EXPECT_EQ(1, n->num_moves);
}

TEST_CASE(should_not_destroy)
{
    Counter* c = nullptr;
    {
        AK::NeverDestroyed<Counter> n {};
        c = &n.get();
    }
    EXPECT_EQ(0, c->num_destroys);
}

TEST_CASE(should_provide_dereference_operator)
{
    AK::NeverDestroyed<Counter> n {};
    EXPECT_EQ(0, n->num_destroys);
}

TEST_CASE(should_provide_indirection_operator)
{
    AK::NeverDestroyed<Counter> n {};
    EXPECT_EQ(0, (*n).num_destroys);
}

TEST_CASE(should_provide_basic_getter)
{
    AK::NeverDestroyed<Counter> n {};
    EXPECT_EQ(0, n.get().num_destroys);
}

TEST_MAIN(NeverDestroyed)
