/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/NonElementParentNode.h>
#include <LibWeb/DOM/ParentNode.h>

namespace Web::DOM {

class DocumentFragment
    : public ParentNode
    , public NonElementParentNode<DocumentFragment> {
    WEB_PLATFORM_OBJECT(DocumentFragment, ParentNode);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentFragment>> construct_impl(JS::Realm& realm);

    virtual ~DocumentFragment() override = default;

    virtual FlyString node_name() const override { return "#document-fragment"_fly_string; }

    Element* host() { return m_host.ptr(); }
    Element const* host() const { return m_host.ptr(); }

    void set_host(Element*);

protected:
    explicit DocumentFragment(Document& document);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

private:
    // https://dom.spec.whatwg.org/#concept-documentfragment-host
    JS::GCPtr<Element> m_host;
};

template<>
inline bool Node::fast_is<DocumentFragment>() const { return is_document_fragment(); }

}
