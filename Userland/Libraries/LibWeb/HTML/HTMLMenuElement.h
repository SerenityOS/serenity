/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMenuElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMenuElement, HTMLElement);

public:
    virtual ~HTMLMenuElement() override;

private:
    HTMLMenuElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLMenuElement, Web::HTML)
