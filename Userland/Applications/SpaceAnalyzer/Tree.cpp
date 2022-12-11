/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tree.h"
#include <AK/QuickSort.h>

void TreeNode::sort_children_by_area() const
{
    if (m_children) {
        Vector<TreeNode>* children = const_cast<Vector<TreeNode>*>(m_children.ptr());
        quick_sort(*children, [](auto& a, auto& b) { return b.m_area < a.m_area; });
    }
}
