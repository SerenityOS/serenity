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

struct MountInfo {
    DeprecatedString mount_point;
    DeprecatedString source;
};

class TreeNode final {
public:
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
    HashMap<int, int> populate_filesize_tree(Vector<MountInfo>& mounts, Function<void(size_t)> on_progress);

private:
    long long int update_totals();

    DeprecatedString m_name;
    i64 m_area { 0 };
    OwnPtr<Vector<TreeNode>> m_children;
};

class Tree : public RefCounted<Tree> {
public:
    Tree(DeprecatedString root_name)
        : m_root(move(root_name)) {};
    ~Tree() {};
    TreeNode& root()
    {
        return m_root;
    };

private:
    TreeNode m_root;
};
