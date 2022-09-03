/*
 * Copyright (c) 2020-2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/HTML/Window.h>

namespace Web::DOM {

DocumentFragment::DocumentFragment(Document& document)
    : ParentNode(document, NodeType::DOCUMENT_FRAGMENT_NODE)
{
    set_prototype(&window().cached_web_prototype("DocumentFragment"));
}

void DocumentFragment::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_host.ptr());
}

void DocumentFragment::set_host(Web::DOM::Element* element)
{
    m_host = element;
}

// https://dom.spec.whatwg.org/#dom-documentfragment-documentfragment
JS::NonnullGCPtr<DocumentFragment> DocumentFragment::create_with_global_object(HTML::Window& window)
{
    return *window.heap().allocate<DocumentFragment>(window.realm(), window.associated_document());
}

}
