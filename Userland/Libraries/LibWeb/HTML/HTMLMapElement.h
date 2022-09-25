/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMapElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMapElement, HTMLElement);

public:
    virtual ~HTMLMapElement() override;

private:
    HTMLMapElement(DOM::Document&, DOM::QualifiedName);
};

}
