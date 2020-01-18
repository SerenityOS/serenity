/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/HashMap.h>

TEST_CASE(construct)
{
    typedef HashMap<int, int> IntIntMap;
    EXPECT(IntIntMap().is_empty());
    EXPECT_EQ(IntIntMap().size(), 0);
}

TEST_CASE(populate)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3);
}

TEST_CASE(range_loop)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    int loop_counter = 0;
    for (auto& it : number_to_string) {
        EXPECT_EQ(it.value.is_null(), false);
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(map_remove)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    number_to_string.remove(1);
    EXPECT_EQ(number_to_string.size(), 2);
    EXPECT(number_to_string.find(1) == number_to_string.end());

    number_to_string.remove(3);
    EXPECT_EQ(number_to_string.size(), 1);
    EXPECT(number_to_string.find(3) == number_to_string.end());
    EXPECT(number_to_string.find(2) != number_to_string.end());
}

TEST_CASE(case_insensitive)
{
    HashMap<String, int, CaseInsensitiveStringTraits> casemap;
    EXPECT_EQ(String("nickserv").to_lowercase(), String("NickServ").to_lowercase());
    casemap.set("nickserv", 3);
    casemap.set("NickServ", 3);
    EXPECT_EQ(casemap.size(), 1);
}

TEST_CASE(assert_on_iteration_during_clear)
{
    struct Object {
        ~Object()
        {
            m_map->begin();
        }
        HashMap<int, Object>* m_map;
    };
    HashMap<int, Object> map;
    map.set(0, { &map });
    map.clear();
}

TEST_CASE(hashmap_of_nonnullownptr_get)
{
    struct Object {
        Object(const String& s) : string(s) {}
        String string;
    };

    HashMap<int, NonnullOwnPtr<Object>> objects;
    objects.set(1, make<Object>("One"));
    objects.set(2, make<Object>("Two"));
    objects.set(3, make<Object>("Three"));

    {
        auto x = objects.get(2);
        EXPECT_EQ(x.has_value(), true);
        EXPECT_EQ(x.value()->string, "Two");
    }

    {
        // Do it again to make sure that peeking into the map above didn't
        // remove the value from the map.
        auto x = objects.get(2);
        EXPECT_EQ(x.has_value(), true);
        EXPECT_EQ(x.value()->string, "Two");
    }

    EXPECT_EQ(objects.size(), 3);
}

TEST_MAIN(HashMap)
