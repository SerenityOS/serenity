/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDataElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDataElement, HTMLElement);

public:
    virtual ~HTMLDataElement() override;

private:
    HTMLDataElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLDataElement, Web::HTML)
