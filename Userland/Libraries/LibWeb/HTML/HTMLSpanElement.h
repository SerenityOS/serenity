/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSpanElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLSpanElement, HTMLElement);

public:
    virtual ~HTMLSpanElement() override;

private:
    HTMLSpanElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLSpanElement, Web::HTML)
