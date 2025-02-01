/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Math.h>
#include <AK/OwnPtr.h>
#include <LibGfx/ImageFormats/JPEG2000TagTree.h>

namespace Gfx::JPEG2000 {

// Tag trees are used to store the code-block inclusion bits and the zero bit-plane information.
// B.10.2 Tag trees
// "At every node of this tree the minimum integer of the (up to four) nodes below it is recorded. [...]
//  Level 0 is the lowest level of the tag tree; it contains the top node. [...]
//  Each node has a [...] current value, [...] initialized to zero. A 0 bit in the tag tree means that the minimum
//  (or the value in the case of the highest level) is larger than the current value and a 1 bit means that the minimum
//  (or the value in the case of the highest level) is equal to the current value.
//  For each contiguous 0 bit in the tag tree the current value is incremented by one.
//  Nodes at higher levels cannot be coded until lower level node values are fixed (i.e, a 1 bit is coded). [...]
//  Only the information needed for the current code-block is stored at the current point in the packet header."
// The example in Figure B.13 / Table B.5 is useful to understand what exactly "only the information needed" means.
struct TagTreeNode {
    u32 value { 0 };
    enum State {
        Pending,
        Final,
    };
    State state { Pending };
    Array<OwnPtr<TagTreeNode>, 4> children {};
    u32 level { 0 }; // 0 for leaf nodes, 1 for the next level, etc.

    bool is_leaf() const { return level == 0; }

    ErrorOr<u32> read_value(u32 x, u32 y, Function<ErrorOr<bool>()> const& read_bit, u32 start_value, Optional<u32> stop_at = {})
    {
        value = max(value, start_value);
        while (true) {
            if (stop_at.has_value() && value == stop_at.value())
                return value;

            if (state == Final) {
                if (is_leaf())
                    return value;
                u32 x_index = (x >> (level - 1)) & 1;
                u32 y_index = (y >> (level - 1)) & 1;
                return children[y_index * 2 + x_index]->read_value(x, y, read_bit, value, stop_at);
            }

            bool bit = TRY(read_bit());
            if (!bit)
                value++;
            else
                state = Final;
        }
    }

    static ErrorOr<NonnullOwnPtr<TagTreeNode>> create(u32 x_count, u32 y_count, u32 level)
    {
        VERIFY(x_count > 0);
        VERIFY(y_count > 0);

        auto node = TRY(try_make<TagTreeNode>());
        node->level = level;
        if (node->is_leaf()) {
            VERIFY(x_count == 1);
            VERIFY(y_count == 1);
            return node;
        }

        u32 top_left_x_child_count = min(x_count, 1u << (max(level, 1) - 1));
        u32 top_left_y_child_count = min(y_count, 1u << (max(level, 1) - 1));
        for (u32 y = 0; y < 2; ++y) {
            for (u32 x = 0; x < 2; ++x) {
                u32 child_x_count = x == 1 ? x_count - top_left_x_child_count : top_left_x_child_count;
                u32 child_y_count = y == 1 ? y_count - top_left_y_child_count : top_left_y_child_count;
                if (child_x_count == 0 || child_y_count == 0)
                    continue;
                node->children[y * 2 + x] = TRY(create(child_x_count, child_y_count, level - 1));
            }
        }
        return node;
    }
};

TagTree::TagTree(NonnullOwnPtr<TagTreeNode> root)
    : m_root(move(root))
{
}

TagTree::TagTree(TagTree&&) = default;
TagTree::~TagTree() = default;

ErrorOr<TagTree> TagTree::create(u32 x_count, u32 y_count)
{
    auto level = ceil(log2(max(x_count, y_count)));
    return TagTree { TRY(TagTreeNode::create(x_count, y_count, level)) };
}

ErrorOr<u32> TagTree::read_value(u32 x, u32 y, Function<ErrorOr<bool>()> const& read_bit, Optional<u32> stop_at) const
{
    return m_root->read_value(x, y, read_bit, m_root->value, stop_at);
}

}
