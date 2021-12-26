/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

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
