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

#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/ParentNode.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Layout/LayoutTable.h>
#include <LibHTML/Layout/LayoutText.h>
#include <LibHTML/Layout/LayoutTreeBuilder.h>

LayoutTreeBuilder::LayoutTreeBuilder()
{
}

static RefPtr<LayoutNode> create_layout_tree(Node& node, const StyleProperties* parent_style)
{
    auto layout_node = node.create_layout_node(parent_style);
    if (!layout_node)
        return nullptr;

    if (!node.has_children())
        return layout_node;

    NonnullRefPtrVector<LayoutNode> layout_children;
    bool have_inline_children = false;
    bool have_block_children = false;

    to<ParentNode>(node).for_each_child([&](Node& child) {
        auto layout_child = create_layout_tree(child, &layout_node->style());
        if (!layout_child)
            return;
        if (layout_child->is_inline())
            have_inline_children = true;
        if (layout_child->is_block())
            have_block_children = true;
        layout_children.append(layout_child.release_nonnull());
    });

    for (auto& layout_child : layout_children) {
        if (have_block_children && have_inline_children && layout_child.is_inline()) {
            if (is<LayoutText>(layout_child) && to<LayoutText>(layout_child).text_for_style(*parent_style) == " ")
                continue;
            layout_node->inline_wrapper().append_child(layout_child);
        } else {
            layout_node->append_child(layout_child);
        }
    }

    if (have_inline_children && !have_block_children)
        layout_node->set_children_are_inline(true);

    // FIXME: This is really hackish. Some layout nodes don't care about inline children.
    if (is<LayoutTable>(layout_node))
        layout_node->set_children_are_inline(false);

    return layout_node;
}

RefPtr<LayoutNode> LayoutTreeBuilder::build(Node& node)
{
    // FIXME: Support building partial trees.
    ASSERT(is<Document>(node));
    return create_layout_tree(node, nullptr);
}
