/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/Document.h>

namespace Web::DOM {

class DOMImplementation final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMImplementation, Bindings::PlatformObject);

public:
    [[nodiscard]] static JS::NonnullGCPtr<DOMImplementation> create(Document&);
    virtual ~DOMImplementation();

    WebIDL::ExceptionOr<JS::NonnullGCPtr<Document>> create_document(DeprecatedString const&, DeprecatedString const&, JS::GCPtr<DocumentType>) const;
    JS::NonnullGCPtr<Document> create_html_document(DeprecatedString const& title) const;
    WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentType>> create_document_type(DeprecatedString const& qualified_name, DeprecatedString const& public_id, DeprecatedString const& system_id);

    // https://dom.spec.whatwg.org/#dom-domimplementation-hasfeature
    bool has_feature() const { return true; }

private:
    explicit DOMImplementation(Document&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    Document& document() { return m_document; }
    Document const& document() const { return m_document; }

    JS::NonnullGCPtr<Document> m_document;
};

}
