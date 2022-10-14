/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDivElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDivElement, HTMLElement);

public:
    virtual ~HTMLDivElement() override;

private:
    HTMLDivElement(DOM::Document&, DOM::QualifiedName);
};

}
