/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>

namespace Gfx::JPEG2000 {

struct TagTreeNode;
class TagTree {
public:
    TagTree(TagTree&&);
    ~TagTree();

    static ErrorOr<TagTree> create(u32 x_count, u32 y_count);

    ErrorOr<u32> read_value(u32 x, u32 y, Function<ErrorOr<bool>()> const& read_bit, Optional<u32> stop_at = {}) const;

private:
    TagTree(NonnullOwnPtr<TagTreeNode>);

    NonnullOwnPtr<TagTreeNode> m_root;
};

}
