/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>

struct TreeNode final {
    TreeNode(DeprecatedString name)
        : m_name(move(name)) {};

    DeprecatedString name() const { return m_name; }
    i64 area() const { return m_area; }
    size_t num_children() const
    {
        if (m_children) {
            return m_children->size();
        }
        return 0;
    }
    TreeNode const& child_at(size_t i) const { return m_children->at(i); }
    void sort_children_by_area() const;

    DeprecatedString m_name;
    i64 m_area { 0 };
    OwnPtr<Vector<TreeNode>> m_children;
};

struct Tree : public RefCounted<Tree> {
    Tree(DeprecatedString root_name)
        : m_root(move(root_name)) {};
    ~Tree() {};
    TreeNode m_root;
    TreeNode const& root() const
    {
        return m_root;
    };
};
