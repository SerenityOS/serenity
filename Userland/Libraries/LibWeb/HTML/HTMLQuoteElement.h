/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLQuoteElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLQuoteElementWrapper;

    HTMLQuoteElement(DOM::Document&, QualifiedName);
    virtual ~HTMLQuoteElement() override;
};

}
