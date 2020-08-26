/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

#include <AK/Checked.h>

// These tests only check whether the usual operator semantics work.
// TODO: Add tests about the actual `Check`ing itself!

TEST_CASE(address_identity)
{
    Checked<int> a = 4;
    Checked<int> b = 5;
    EXPECT_EQ(&a == &a, true);
    EXPECT_EQ(&a == &b, false);
    EXPECT_EQ(&a != &a, false);
    EXPECT_EQ(&a != &b, true);
}

TEST_CASE(operator_identity)
{
    Checked<int> a = 4;
    EXPECT_EQ(a == 4, true);
    EXPECT_EQ(a == 5, false);
    EXPECT_EQ(a != 4, false);
    EXPECT_EQ(a != 5, true);
}

TEST_CASE(operator_incr)
{
    Checked<int> a = 4;
    EXPECT_EQ(++a, 5);
    EXPECT_EQ(++a, 6);
    EXPECT_EQ(++a, 7);
    EXPECT_EQ(a++, 7);
    EXPECT_EQ(a++, 8);
    EXPECT_EQ(a++, 9);
    EXPECT_EQ(a, 10);
    // TODO: If decrementing gets supported, test it.
}

TEST_CASE(operator_cmp)
{
    Checked<int> a = 4;
    EXPECT_EQ(a > 3, true);
    EXPECT_EQ(a < 3, false);
    EXPECT_EQ(a >= 3, true);
    EXPECT_EQ(a <= 3, false);
    EXPECT_EQ(a > 4, false);
    EXPECT_EQ(a < 4, false);
    EXPECT_EQ(a >= 4, true);
    EXPECT_EQ(a <= 4, true);
    EXPECT_EQ(a > 5, false);
    EXPECT_EQ(a < 5, true);
    EXPECT_EQ(a >= 5, false);
    EXPECT_EQ(a <= 5, true);
}

TEST_CASE(operator_arith)
{
    Checked<int> a = 12;
    Checked<int> b = 345;
    EXPECT_EQ(a + b, 357);
    EXPECT_EQ(b + a, 357);
    EXPECT_EQ(a - b, -333);
    EXPECT_EQ(b - a, 333);
    EXPECT_EQ(a * b, 4140);
    EXPECT_EQ(b * a, 4140);
    EXPECT_EQ(a / b, 0);
    EXPECT_EQ(b / a, 28);
}

TEST_MAIN(Checked)
