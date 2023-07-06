/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/MathML/MathMLElement.h>
#include <LibWeb/MathML/TagNames.h>

namespace Web::MathML {

MathMLElement::~MathMLElement() = default;

MathMLElement::MathMLElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : DOM::Element(document, move(qualified_name))
{
}

void MathMLElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MathMLElementPrototype>(realm, "MathMLElement"));

    m_dataset = MUST(HTML::DOMStringMap::create(*this));
}

Optional<ARIA::Role> MathMLElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-math
    if (local_name() == TagNames::math.to_deprecated_fly_string())
        return ARIA::Role::math;
    return {};
}

}
