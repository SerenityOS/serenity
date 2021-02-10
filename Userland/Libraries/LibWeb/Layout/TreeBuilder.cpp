/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableRowBox.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/TreeBuilder.h>

namespace Web::Layout {

TreeBuilder::TreeBuilder()
{
}

// The insertion_parent_for_*() functions maintain the invariant that block-level boxes must have either
// only block-level children or only inline-level children.

static Layout::Node& insertion_parent_for_inline_node(Layout::NodeWithStyle& layout_parent)
{
    if (layout_parent.is_inline() && !layout_parent.is_inline_block())
        return layout_parent;

    if (!layout_parent.has_children() || layout_parent.children_are_inline())
        return layout_parent;

    // Parent has block-level children, insert into an anonymous wrapper block (and create it first if needed)
    if (!layout_parent.last_child()->is_anonymous() || !layout_parent.last_child()->children_are_inline()) {
        layout_parent.append_child(layout_parent.create_anonymous_wrapper());
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
    layout_parent.append_child(adopt(*new BlockBox(layout_node.document(), nullptr, layout_parent.computed_values().clone_inherited_values())));
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
    if (dom_node.parent_or_shadow_host() && !dom_node.parent_or_shadow_host()->layout_node())
        return;

    auto layout_node = dom_node.create_layout_node();
    if (!layout_node)
        return;

    if (!dom_node.parent_or_shadow_host()) {
        m_layout_root = layout_node;
    } else {
        if (layout_node->is_inline()) {
            // Inlines can be inserted into the nearest ancestor.
            auto& insertion_point = insertion_parent_for_inline_node(*m_parent_stack.last());
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

    auto* shadow_root = is<DOM::Element>(dom_node) ? downcast<DOM::Element>(dom_node).shadow_root() : nullptr;

    if ((dom_node.has_children() || shadow_root) && layout_node->can_have_children()) {
        push_parent(downcast<NodeWithStyle>(*layout_node));
        if (shadow_root)
            create_layout_tree(*shadow_root);
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
            m_parent_stack.prepend(downcast<NodeWithStyle>(ancestor));
    }

    create_layout_tree(dom_node);

    if (auto* root = dom_node.document().layout_node())
        fixup_tables(*root);

    return move(m_layout_root);
}

template<CSS::Display display, typename Callback>
void TreeBuilder::for_each_in_tree_with_display(NodeWithStyle& root, Callback callback)
{
    root.for_each_in_subtree_of_type<Box>([&](auto& box) {
        if (box.computed_values().display() == display)
            callback(box);
        return IterationDecision::Continue;
    });
}

void TreeBuilder::fixup_tables(NodeWithStyle& root)
{
    // NOTE: Even if we only do a partial build, we always do fixup from the root.

    remove_irrelevant_boxes(root);
    generate_missing_child_wrappers(root);
    generate_missing_parents(root);
}

void TreeBuilder::remove_irrelevant_boxes(NodeWithStyle& root)
{
    // The following boxes are discarded as if they were display:none:

    NonnullRefPtrVector<Box> to_remove;

    // Children of a table-column.
    for_each_in_tree_with_display<CSS::Display::TableColumn>(root, [&](Box& table_column) {
        table_column.for_each_child([&](auto& child) {
            to_remove.append(child);
        });
    });

    // Children of a table-column-group which are not a table-column.
    for_each_in_tree_with_display<CSS::Display::TableColumnGroup>(root, [&](Box& table_column_group) {
        table_column_group.for_each_child([&](auto& child) {
            if (child.computed_values().display() != CSS::Display::TableColumn)
                to_remove.append(child);
        });
    });

    // FIXME:
    // Anonymous inline boxes which contain only white space and are between two immediate siblings each of which is a table-non-root box.
    // Anonymous inline boxes which meet all of the following criteria:
    // - they contain only white space
    // - they are the first and/or last child of a tabular container
    // - whose immediate sibling, if any, is a table-non-root box

    for (auto& box : to_remove)
        box.parent()->remove_child(box);
}

static bool is_table_track(CSS::Display display)
{
    return display == CSS::Display::TableRow || display == CSS::Display::TableColumn;
}

static bool is_table_track_group(CSS::Display display)
{
    return display == CSS::Display::TableRowGroup || display == CSS::Display::TableColumnGroup;
}

static bool is_not_proper_table_child(const Node& node)
{
    if (!node.has_style())
        return true;
    auto display = node.computed_values().display();
    return !is_table_track_group(display) && !is_table_track(display) && display != CSS::Display::TableCaption;
}

static bool is_not_table_row(const Node& node)
{
    if (!node.has_style())
        return true;
    auto display = node.computed_values().display();
    return display != CSS::Display::TableRow;
}

static bool is_not_table_cell(const Node& node)
{
    if (!node.has_style())
        return true;
    auto display = node.computed_values().display();
    return display != CSS::Display::TableCell;
}

template<typename Matcher, typename Callback>
static void for_each_sequence_of_consecutive_children_matching(NodeWithStyle& parent, Matcher matcher, Callback callback)
{
    NonnullRefPtrVector<Node> sequence;
    Node* next_sibling = nullptr;
    for (auto* child = parent.first_child(); child; child = next_sibling) {
        next_sibling = child->next_sibling();
        if (matcher(*child)) {
            sequence.append(*child);
        } else {
            if (!sequence.is_empty()) {
                callback(sequence, next_sibling);
                sequence.clear();
            }
        }
    }
    if (!sequence.is_empty())
        callback(sequence, nullptr);
}

template<typename WrapperBoxType>
static void wrap_in_anonymous(NonnullRefPtrVector<Node>& sequence, Node* nearest_sibling)
{
    ASSERT(!sequence.is_empty());
    auto& parent = *sequence.first().parent();
    auto computed_values = parent.computed_values().clone_inherited_values();
    static_cast<CSS::MutableComputedValues&>(computed_values).set_display(WrapperBoxType::static_display());
    auto wrapper = adopt(*new WrapperBoxType(parent.document(), nullptr, move(computed_values)));
    for (auto& child : sequence) {
        parent.remove_child(child);
        wrapper->append_child(child);
    }
    if (nearest_sibling)
        parent.insert_before(move(wrapper), *nearest_sibling);
    else
        parent.append_child(move(wrapper));
}

void TreeBuilder::generate_missing_child_wrappers(NodeWithStyle& root)
{
    // An anonymous table-row box must be generated around each sequence of consecutive children of a table-root box which are not proper table child boxes.
    for_each_in_tree_with_display<CSS::Display::Table>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_proper_table_child, [&](auto sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
        });
    });

    // An anonymous table-row box must be generated around each sequence of consecutive children of a table-row-group box which are not table-row boxes.
    for_each_in_tree_with_display<CSS::Display::TableRowGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
        });
    });

    // An anonymous table-cell box must be generated around each sequence of consecutive children of a table-row box which are not table-cell boxes. !Testcase
    for_each_in_tree_with_display<CSS::Display::TableRow>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_cell, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableCellBox>(sequence, nearest_sibling);
        });
    });
}

void TreeBuilder::generate_missing_parents(NodeWithStyle&)
{
    // FIXME: Implement.
}

}
