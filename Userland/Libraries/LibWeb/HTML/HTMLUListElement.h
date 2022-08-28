/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLUListElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLUListElement, HTMLElement);

public:
    virtual ~HTMLUListElement() override;

private:
    HTMLUListElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLUListElement, Web::HTML)
