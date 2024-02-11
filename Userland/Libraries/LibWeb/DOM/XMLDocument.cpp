/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/XMLDocumentPrototype.h>
#include <LibWeb/DOM/XMLDocument.h>

namespace Web::DOM {

JS_DEFINE_ALLOCATOR(XMLDocument);

JS::NonnullGCPtr<XMLDocument> XMLDocument::create(JS::Realm& realm, URL const& url)
{
    return realm.heap().allocate<XMLDocument>(realm, realm, url);
}

XMLDocument::XMLDocument(JS::Realm& realm, URL const& url)
    : Document(realm, url)
{
}

void XMLDocument::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::XMLDocumentPrototype>(realm, "XMLDocument"_fly_string));
}

}
