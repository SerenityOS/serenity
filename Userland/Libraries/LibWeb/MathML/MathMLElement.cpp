/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/MathML/MathMLElement.h>
#include <LibWeb/MathML/TagNames.h>

namespace Web::MathML {

JS_DEFINE_ALLOCATOR(MathMLElement);

MathMLElement::~MathMLElement() = default;

MathMLElement::MathMLElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : DOM::Element(document, move(qualified_name))
{
}

void MathMLElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MathMLElementPrototype>(realm, "MathMLElement"_fly_string));

    m_dataset = HTML::DOMStringMap::create(*this);
}

Optional<ARIA::Role> MathMLElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-math
    if (local_name() == TagNames::math)
        return ARIA::Role::math;
    return {};
}

void MathMLElement::focus()
{
    dbgln("(STUBBED) MathMLElement::focus()");
}

void MathMLElement::blur()
{
    dbgln("(STUBBED) MathMLElement::blur()");
}

}
