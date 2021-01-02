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

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/TreeBuilder.h>

namespace Web::Layout {

TreeBuilder::TreeBuilder()
{
}

static NonnullRefPtr<CSS::StyleProperties> style_for_anonymous_block(Node& parent_box)
{
    auto new_style = CSS::StyleProperties::create();

    parent_box.specified_style().for_each_property([&](auto property_id, auto& value) {
        if (CSS::StyleResolver::is_inherited_property(property_id))
            new_style->set_property(property_id, value);
    });

    return new_style;
}

// The insertion_parent_for_*() functions maintain the invariant that block-level boxes must have either
// only block-level children or only inline-level children.

static Layout::Node& insertion_parent_for_inline_node(Layout::Node& layout_parent, Layout::Node& layout_node)
{
    if (layout_parent.is_inline())
        return layout_parent;

    if (!layout_parent.has_children() || layout_parent.children_are_inline())
        return layout_parent;

    // Parent has block-level children, insert into an anonymous wrapper block (and create it first if needed)
    if (!layout_parent.last_child()->is_anonymous() || !layout_parent.last_child()->children_are_inline()) {
        layout_parent.append_child(adopt(*new BlockBox(layout_node.document(), nullptr, style_for_anonymous_block(layout_parent))));
    }
    return *layout_parent.last_child();
}

static Layout::Node& insertion_parent_for_block_node(Layout::Node& layout_parent, Layout::Node& layout_node)
{
    if (!layout_parent.has_children()) {
        // Parent block has no children, insert this block into parent.
        return layout_parent;
    }

    if (!layout_parent.children_are_inline()) {
        // Parent block has block-level children, insert this block into parent.
        return layout_parent;
    }

    // Parent block has inline-level children (our siblings).
    // First move these siblings into an anonymous wrapper block.
    NonnullRefPtrVector<Layout::Node> children;
    while (RefPtr<Layout::Node> child = layout_parent.first_child()) {
        layout_parent.remove_child(*child);
        children.append(child.release_nonnull());
    }
    layout_parent.append_child(adopt(*new BlockBox(layout_node.document(), nullptr, style_for_anonymous_block(layout_parent))));
    layout_parent.set_children_are_inline(false);
    for (auto& child : children) {
        layout_parent.last_child()->append_child(child);
    }
    layout_parent.last_child()->set_children_are_inline(true);
    // Then it's safe to insert this block into parent.
    return layout_parent;
}

void TreeBuilder::create_layout_tree(DOM::Node& dom_node)
{
    // If the parent doesn't have a layout node, we don't need one either.
    if (dom_node.parent() && !dom_node.parent()->layout_node())
        return;

    const CSS::StyleProperties* parent_style = m_parent_stack.is_empty() ? nullptr : &m_parent_stack.last()->specified_style();

    auto layout_node = dom_node.create_layout_node(parent_style);
    if (!layout_node)
        return;

    // Discard empty whitespace nodes. This might not be ideal for correctness, but it does make the tree nicer.
    if (is<TextNode>(*layout_node) && downcast<TextNode>(*layout_node).text_for_style(*parent_style) == " ")
        return;

    if (!dom_node.parent()) {
        m_layout_root = layout_node;
    } else {
        if (layout_node->is_inline()) {
            // Inlines can be inserted into the nearest ancestor.
            auto& insertion_point = insertion_parent_for_inline_node(*m_parent_stack.last(), *layout_node);
            insertion_point.append_child(*layout_node);
            insertion_point.set_children_are_inline(true);
        } else {
            // Non-inlines can't be inserted into an inline parent, so find the nearest non-inline ancestor.
            auto& nearest_non_inline_ancestor = [&]() -> Layout::Node& {
                for (ssize_t i = m_parent_stack.size() - 1; i >= 0; --i) {
                    if (!m_parent_stack[i]->is_inline() || m_parent_stack[i]->is_inline_block())
                        return *m_parent_stack[i];
                }
                ASSERT_NOT_REACHED();
            }();
            auto& insertion_point = insertion_parent_for_block_node(nearest_non_inline_ancestor, *layout_node);
            insertion_point.append_child(*layout_node);
            insertion_point.set_children_are_inline(false);
        }
    }

    if (dom_node.has_children() && layout_node->can_have_children()) {
        push_parent(*layout_node);
        downcast<DOM::ParentNode>(dom_node).for_each_child([&](auto& dom_child) {
            create_layout_tree(dom_child);
        });
        pop_parent();
    }
}

RefPtr<Node> TreeBuilder::build(DOM::Node& dom_node)
{
    if (dom_node.parent()) {
        // We're building a partial layout tree, so start by building up the stack of parent layout nodes.
        for (auto* ancestor = dom_node.parent()->layout_node(); ancestor; ancestor = ancestor->parent())
            m_parent_stack.prepend(ancestor);
    }

    create_layout_tree(dom_node);
    return move(m_layout_root);
}

}
