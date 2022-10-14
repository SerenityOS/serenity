/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLQuoteElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLQuoteElement, HTMLElement);

public:
    virtual ~HTMLQuoteElement() override;

private:
    HTMLQuoteElement(DOM::Document&, DOM::QualifiedName);
};

}
