/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDetailsElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDetailsElement, HTMLElement);

public:
    virtual ~HTMLDetailsElement() override;

private:
    HTMLDetailsElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLDetailsElement, Web::HTML)
