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

// https://dom.spec.whatwg.org/#nodeiterator
class NodeIterator
    : public RefCounted<NodeIterator>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::NodeIteratorWrapper;

    virtual ~NodeIterator() override;

    static NonnullRefPtr<NodeIterator> create(Node& root, unsigned what_to_show, RefPtr<NodeFilter>);

    NonnullRefPtr<Node> root() { return m_root; }
    NonnullRefPtr<Node> reference_node() { return m_reference; }
    bool pointer_before_reference_node() const { return m_pointer_before_reference; }
    unsigned what_to_show() const { return m_what_to_show; }

    NodeFilter* filter() { return m_filter; }

    JS::ThrowCompletionOr<RefPtr<Node>> next_node();
    JS::ThrowCompletionOr<RefPtr<Node>> previous_node();

    void detach();

    void run_pre_removing_steps(Node&);

private:
    NodeIterator(Node& root);

    enum class Direction {
        Next,
        Previous,
    };

    JS::ThrowCompletionOr<RefPtr<Node>> traverse(Direction);

    JS::ThrowCompletionOr<NodeFilter::Result> filter(Node&);

    // https://dom.spec.whatwg.org/#concept-traversal-root
    NonnullRefPtr<DOM::Node> m_root;

    // https://dom.spec.whatwg.org/#nodeiterator-reference
    NonnullRefPtr<DOM::Node> m_reference;

    // https://dom.spec.whatwg.org/#nodeiterator-pointer-before-reference
    bool m_pointer_before_reference { true };

    // https://dom.spec.whatwg.org/#concept-traversal-whattoshow
    unsigned m_what_to_show { 0 };

    // https://dom.spec.whatwg.org/#concept-traversal-filter
    RefPtr<DOM::NodeFilter> m_filter;

    // https://dom.spec.whatwg.org/#concept-traversal-active
    bool m_active { false };
};

}
