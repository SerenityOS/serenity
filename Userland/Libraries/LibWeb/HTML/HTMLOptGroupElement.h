/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLOptGroupElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLOptGroupElement, HTMLElement);

public:
    virtual ~HTMLOptGroupElement() override;

private:
    HTMLOptGroupElement(DOM::Document&, DOM::QualifiedName);
};

}
