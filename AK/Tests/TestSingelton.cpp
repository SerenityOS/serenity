/*
 * Copyright (c) 2020, Muhammad Zahalqa <m@tryfinally.com>
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

#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/Singleton.h>
#include <AK/String.h>

class Uno {
public:
    Uno(int c)
        : m_count(c)
    {
    }

    int count() const { return m_count; }

private:
    int m_count;
};

TEST_CASE(ther_can_be_only_one)
{
    auto& c = Singleton<Uno>::the(42);
    auto& d = Singleton<Uno>::the(1000);

    EXPECT_EQ(c.count(), 42);
    EXPECT_EQ(d.count(), 42);
    EXPECT_EQ(&c, &d);
}

TEST_CASE(ther_can_be_only_one_hashmap_string_int)
{
    auto& map1 = Singleton<HashMap<String, int>>::the();
    auto& map2 = Singleton<HashMap<String, int>>::the();
    EXPECT_EQ(&map1, &map2);

    map1.set("C++", 17);
    map1.set("Java", 14);
    map2.set("Fortran", 77);
    EXPECT_EQ(map1.size(), 3u);
    EXPECT_EQ(map1.size(), map2.size());
}

TEST_CASE(two_distinct_singeltons)
{
    auto& map1 = Singleton<HashTable<String>>::the(100);
    map1.set("SerenityOS");
    map1.set("BeOS");
    map1.set("Os9");
    EXPECT_EQ(map1.size(), 3u);
    EXPECT_EQ(map1.capacity(), 100u);

    auto& map2 = Singleton<HashTable<int>>::the(42);
    map2.set(42);
    EXPECT_EQ(map2.size(), 1u);
    EXPECT_EQ(map2.capacity(), 42u);
    Singleton<HashTable<int>>::the().set(888);
    Singleton<HashTable<int>>::the().set(111);
    EXPECT_EQ(map2.size(), 3u);
}

TEST_MAIN(Signelton)
