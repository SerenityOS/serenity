/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCountForwarder.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/Document.h>

namespace Web::DOM {

class DOMImplementation final
    : public RefCountForwarder<Document>
    , public Weakable<DOMImplementation>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMImplementationWrapper;

    static NonnullOwnPtr<DOMImplementation> create(Badge<Document>, Document& document)
    {
        return adopt_own(*new DOMImplementation(document));
    }

    // FIXME: Add optional DocumentType once supported by IDL
    NonnullRefPtr<Document> create_document(const String&, const String&) const;
    NonnullRefPtr<Document> create_html_document(const String& title) const;
    NonnullRefPtr<DocumentType> create_document_type(String const& qualified_name, String const& public_id, String const& system_id);

    // https://dom.spec.whatwg.org/#dom-domimplementation-hasfeature
    bool has_feature() const { return true; }

private:
    explicit DOMImplementation(Document&);

    Document& document() { return ref_count_target(); }
    Document const& document() const { return ref_count_target(); }
};

}
