/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/Document.h>

namespace Web::DOM {

class DOMImplementation final : public Bindings::PlatformObject {
    JS_OBJECT(DOMImplementation, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<DOMImplementation> create(Document&);
    explicit DOMImplementation(Document&);
    virtual ~DOMImplementation();

    DOMImplementation& impl() { return *this; }

    ExceptionOr<NonnullRefPtr<Document>> create_document(String const&, String const&, RefPtr<DocumentType>) const;
    NonnullRefPtr<Document> create_html_document(String const& title) const;
    ExceptionOr<NonnullRefPtr<DocumentType>> create_document_type(String const& qualified_name, String const& public_id, String const& system_id);

    // https://dom.spec.whatwg.org/#dom-domimplementation-hasfeature
    bool has_feature() const { return true; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Document& document() { return m_document; }
    Document const& document() const { return m_document; }

    Document& m_document;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::DOMImplementation& object) { return &object; }
using DOMImplementationWrapper = Web::DOM::DOMImplementation;
}
