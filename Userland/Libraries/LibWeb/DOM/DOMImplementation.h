/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
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
        return adopt(*new DOMImplementation(document));
    }

    const NonnullRefPtr<Document> create_html_document(const String& title) const;

    // https://dom.spec.whatwg.org/#dom-domimplementation-hasfeature
    bool has_feature() const { return true; }

private:
    explicit DOMImplementation(Document&);

    Document& m_document;
};

}
