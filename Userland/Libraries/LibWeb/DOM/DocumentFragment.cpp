/*
 * Copyright (c) 2020-2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/Window.h>

namespace Web::DOM {

DocumentFragment::DocumentFragment(Document& document)
    : ParentNode(document, NodeType::DOCUMENT_FRAGMENT_NODE)
{
}

DocumentFragment::~DocumentFragment()
{
}

// https://dom.spec.whatwg.org/#dom-documentfragment-documentfragment
NonnullRefPtr<DocumentFragment> DocumentFragment::create_with_global_object(Bindings::WindowObject& window)
{
    return make_ref_counted<DocumentFragment>(window.impl().associated_document());
}

}
