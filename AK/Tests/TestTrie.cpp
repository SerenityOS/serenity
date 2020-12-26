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

#include <AK/Trie.h>

TEST_CASE(normal_behaviour)
{
    Trie<char, String> dictionary('/', "");
    constexpr static const char* data[] { "test", "example", "foo", "foobar" };
    constexpr static const size_t total_chars = 18; // root (1), 'test' (4), 'example' (7), 'foo' (3), 'foobar' (3, "foo" already stored).
    for (auto& entry : data) {
        StringView view { entry };
        auto it = view.begin();
        dictionary.insert(it, view.end(), view, [](auto& parent, auto& it) -> Optional<String> { return String::formatted("{}{}", parent.metadata_value(), *it); });
    }

    size_t i = 0;
    for ([[maybe_unused]] auto& node : dictionary)
        ++i;
    EXPECT_EQ(i, total_chars);

    for (auto& entry : data) {
        StringView view { entry };
        auto it = view.begin();
        auto& node = dictionary.traverse_until_last_accessible_node(it, view.end());
        EXPECT(it.is_end());
        EXPECT(node.metadata().has_value());
        EXPECT_EQ(view, node.metadata_value());
    }

    constexpr static const char* test_data_with_prefix_in_dict[] { "testx", "exampley", "fooa", "foobarb", "fox", "text" };
    for (auto& entry : test_data_with_prefix_in_dict) {
        StringView view { entry };
        auto it = view.begin();
        auto& node = dictionary.traverse_until_last_accessible_node(it, view.end());
        EXPECT(!it.is_end());
        EXPECT(node.metadata().has_value());
        EXPECT(view.starts_with(node.metadata_value()));
    }
}

TEST_CASE(iterate)
{
    Trie<int> bunch_of_numbers { 0 };
    Array<int, 64> input;
    for (size_t i = 0; i < input.size(); ++i)
        input[i] = i;

    bunch_of_numbers.insert(input.begin(), input.end());

    // Iteration order is preorder (order between adjacent nodes is not defined, but parents come before children)
    // in this case, the tree is linear.
    size_t i = 0;
    bool is_root = true;
    for (auto& node : bunch_of_numbers) {
        if (is_root) {
            is_root = false;
            continue;
        }
        EXPECT_EQ(input[i], node.value());
        ++i;
    }
}

TEST_MAIN(AllOf)
