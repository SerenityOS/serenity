/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLQuoteElement.h>

namespace Web::HTML {

HTMLQuoteElement::HTMLQuoteElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLQuoteElement::~HTMLQuoteElement() = default;

void HTMLQuoteElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLQuoteElementPrototype>(realm, "HTMLQuoteElement"));
}

DeprecatedFlyString HTMLQuoteElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-blockquote
    if (local_name() == TagNames::blockquote)
        return DOM::ARIARoleNames::blockquote;
    // https://www.w3.org/TR/html-aria/#el-q
    if (local_name() == TagNames::q)
        return DOM::ARIARoleNames::generic;
    VERIFY_NOT_REACHED();
}

}
