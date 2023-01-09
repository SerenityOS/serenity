/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLModElement.h>

namespace Web::HTML {

HTMLModElement::HTMLModElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLModElement"));
}

HTMLModElement::~HTMLModElement() = default;

DeprecatedFlyString HTMLModElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-del
    if (local_name() == TagNames::del)
        return DOM::ARIARoleNames::deletion;
    // https://www.w3.org/TR/html-aria/#el-ins
    if (local_name() == TagNames::ins)
        return DOM::ARIARoleNames::insertion;
    VERIFY_NOT_REACHED();
}

}
