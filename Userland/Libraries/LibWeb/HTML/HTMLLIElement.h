/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLLIElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLLIElement, HTMLElement);

public:
    virtual ~HTMLLIElement() override;

private:
    HTMLLIElement(DOM::Document&, DOM::QualifiedName);
};

}
