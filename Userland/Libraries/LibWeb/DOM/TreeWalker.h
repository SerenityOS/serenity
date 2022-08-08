/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/NodeFilter.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#treewalker
class TreeWalker
    : public RefCounted<TreeWalker>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::TreeWalkerWrapper;

    static NonnullRefPtr<TreeWalker> create(Node& root, unsigned what_to_show, NodeFilter*);
    virtual ~TreeWalker() override = default;

    NonnullRefPtr<Node> current_node() const;
    void set_current_node(Node&);

    JS::ThrowCompletionOr<RefPtr<Node>> parent_node();
    JS::ThrowCompletionOr<RefPtr<Node>> first_child();
    JS::ThrowCompletionOr<RefPtr<Node>> last_child();
    JS::ThrowCompletionOr<RefPtr<Node>> previous_sibling();
    JS::ThrowCompletionOr<RefPtr<Node>> next_sibling();
    JS::ThrowCompletionOr<RefPtr<Node>> previous_node();
    JS::ThrowCompletionOr<RefPtr<Node>> next_node();

    NonnullRefPtr<Node> root() { return m_root; }

    NodeFilter* filter() { return m_filter.cell(); }

    unsigned what_to_show() const { return m_what_to_show; }

private:
    TreeWalker(Node& root);

    enum class ChildTraversalType {
        First,
        Last,
    };
    JS::ThrowCompletionOr<RefPtr<Node>> traverse_children(ChildTraversalType);

    enum class SiblingTraversalType {
        Next,
        Previous,
    };
    JS::ThrowCompletionOr<RefPtr<Node>> traverse_siblings(SiblingTraversalType);

    JS::ThrowCompletionOr<NodeFilter::Result> filter(Node&);

    // https://dom.spec.whatwg.org/#concept-traversal-root
    NonnullRefPtr<DOM::Node> m_root;

    // https://dom.spec.whatwg.org/#treewalker-current
    NonnullRefPtr<DOM::Node> m_current;

    // https://dom.spec.whatwg.org/#concept-traversal-whattoshow
    unsigned m_what_to_show { 0 };

    // https://dom.spec.whatwg.org/#concept-traversal-filter
    JS::Handle<DOM::NodeFilter> m_filter;

    // https://dom.spec.whatwg.org/#concept-traversal-active
    bool m_active { false };
};

}
