/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLOListElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLOListElement, HTMLElement);

public:
    virtual ~HTMLOListElement() override;

private:
    HTMLOListElement(DOM::Document&, DOM::QualifiedName);
};

}
