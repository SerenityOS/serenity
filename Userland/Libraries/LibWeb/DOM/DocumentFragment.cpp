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
}

JS::ThrowCompletionOr<void> DocumentFragment::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DocumentFragmentPrototype>(realm, "DocumentFragment"));

    return {};
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
JS::NonnullGCPtr<DocumentFragment> DocumentFragment::construct_impl(JS::Realm& realm)
{
    auto& window = verify_cast<HTML::Window>(realm.global_object());
    return realm.heap().allocate<DocumentFragment>(realm, window.associated_document());
}

}
