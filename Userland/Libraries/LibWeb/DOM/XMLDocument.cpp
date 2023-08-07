/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/XMLDocumentPrototype.h>
#include <LibWeb/DOM/XMLDocument.h>

namespace Web::DOM {

XMLDocument::XMLDocument(JS::Realm& realm, AK::URL const& url)
    : Document(realm, url)
{
}

void XMLDocument::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::XMLDocumentPrototype>(realm, "XMLDocument"));
}

}
