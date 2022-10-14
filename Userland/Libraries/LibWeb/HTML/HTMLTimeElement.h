/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTimeElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTimeElement, HTMLElement);

public:
    virtual ~HTMLTimeElement() override;

private:
    HTMLTimeElement(DOM::Document&, DOM::QualifiedName);
};

}
