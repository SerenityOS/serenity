/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/NonElementParentNode.h>
#include <LibWeb/DOM/ParentNode.h>

namespace Web::DOM {

class DocumentFragment
    : public ParentNode
    , public NonElementParentNode<DocumentFragment> {
public:
    using WrapperType = Bindings::DocumentFragmentWrapper;

    static NonnullRefPtr<DocumentFragment> create_with_global_object(Bindings::WindowObject& window);

    explicit DocumentFragment(Document& document);
    virtual ~DocumentFragment() override = default;

    virtual FlyString node_name() const override { return "#document-fragment"; }

    Element* host() { return m_host; }
    Element const* host() const { return m_host; }

    void set_host(Element* host) { m_host = host; }

private:
    // https://dom.spec.whatwg.org/#concept-documentfragment-host
    WeakPtr<Element> m_host;
};

template<>
inline bool Node::fast_is<DocumentFragment>() const { return is_document_fragment(); }

}
