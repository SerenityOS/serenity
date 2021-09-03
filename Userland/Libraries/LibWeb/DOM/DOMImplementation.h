/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullRefPtr.h>
#include <YAK/RefCounted.h>
#include <YAK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

class DOMImplementation final
    : public RefCounted<DOMImplementation>
    , public Weakable<DOMImplementation>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMImplementationWrapper;

    static NonnullRefPtr<DOMImplementation> create(Document& document)
    {
        return adopt_ref(*new DOMImplementation(document));
    }

    // FIXME: Add optional DocumentType once supported by IDL
    NonnullRefPtr<Document> create_document(const String&, const String&) const;
    NonnullRefPtr<Document> create_html_document(const String& title) const;
    NonnullRefPtr<DocumentType> create_document_type(const String&, const String&, const String&) const;

    // https://dom.spec.whatwg.org/#dom-domimplementation-hasfeature
    bool has_feature() const { return true; }

private:
    explicit DOMImplementation(Document&);

    Document& m_document;
};

}
