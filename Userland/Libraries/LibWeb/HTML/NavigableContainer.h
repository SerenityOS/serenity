/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class NavigableContainer : public HTMLElement {
    WEB_PLATFORM_OBJECT(NavigableContainer, HTMLElement);

public:
    static JS::GCPtr<NavigableContainer> navigable_container_with_content_navigable(JS::NonnullGCPtr<Navigable> navigable);

    virtual ~NavigableContainer() override;

    static HashTable<NavigableContainer*>& all_instances();

    JS::GCPtr<Navigable> content_navigable() { return m_content_navigable; }
    JS::GCPtr<Navigable const> content_navigable() const { return m_content_navigable.ptr(); }

    BrowsingContext* nested_browsing_context() { return m_nested_browsing_context; }
    BrowsingContext const* nested_browsing_context() const { return m_nested_browsing_context; }

    const DOM::Document* content_document() const;
    DOM::Document const* content_document_without_origin_check() const;

    HTML::WindowProxy* content_window();

    DOM::Document const* get_svg_document() const;

    void destroy_the_child_navigable();

protected:
    NavigableContainer(DOM::Document&, DOM::QualifiedName);

    virtual void visit_edges(Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/iframe-embed-object.html#shared-attribute-processing-steps-for-iframe-and-frame-elements
    void shared_attribute_processing_steps_for_iframe_and_frame(bool initial_insertion);

    // https://html.spec.whatwg.org/multipage/iframe-embed-object.html#navigate-an-iframe-or-frame
    void navigate_an_iframe_or_frame(JS::NonnullGCPtr<Fetch::Infrastructure::Request>);

    void create_new_nested_browsing_context();

    WebIDL::ExceptionOr<void> create_new_child_navigable();

    JS::GCPtr<BrowsingContext> m_nested_browsing_context;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#content-navigable
    JS::GCPtr<Navigable> m_content_navigable { nullptr };

private:
    virtual bool is_navigable_container() const override { return true; }
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::NavigableContainer>() const { return is_navigable_container(); }
}
