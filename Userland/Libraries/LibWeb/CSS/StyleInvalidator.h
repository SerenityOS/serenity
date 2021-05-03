/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

class StyleInvalidator {
public:
    explicit StyleInvalidator(DOM::Document&);
    ~StyleInvalidator();

private:
    DOM::Document& m_document;
    HashMap<DOM::Element*, Vector<MatchingRule>> m_elements_and_matching_rules_before;
};

}
