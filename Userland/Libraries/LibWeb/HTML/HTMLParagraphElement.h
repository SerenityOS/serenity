/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLParagraphElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLParagraphElement, HTMLElement);

public:
    virtual ~HTMLParagraphElement() override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

private:
    HTMLParagraphElement(DOM::Document&, DOM::QualifiedName);
};

}
