/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <LibWeb/CSS/Display.h>
#include <LibWeb/Forward.h>

namespace Web::Layout {

class TreeBuilder {
public:
    TreeBuilder();

    RefPtr<Layout::Node> build(DOM::Node&);

private:
    struct Context {
        bool has_svg_root = false;
    };

    void create_layout_tree(DOM::Node&, Context&);

    void push_parent(Layout::NodeWithStyle& node) { m_parent_stack.append(&node); }
    void pop_parent() { m_parent_stack.take_last(); }

    template<CSS::Display::Internal, typename Callback>
    void for_each_in_tree_with_internal_display(NodeWithStyle& root, Callback);

    template<CSS::Display::Inside, typename Callback>
    void for_each_in_tree_with_inside_display(NodeWithStyle& root, Callback);

    void fixup_tables(NodeWithStyle& root);
    void remove_irrelevant_boxes(NodeWithStyle& root);
    void generate_missing_child_wrappers(NodeWithStyle& root);
    void generate_missing_parents(NodeWithStyle& root);

    RefPtr<Layout::Node> m_layout_root;
    Vector<Layout::NodeWithStyle*> m_parent_stack;
};

}
