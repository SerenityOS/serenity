/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHeadingElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLHeadingElement, HTMLElement);

public:
    virtual ~HTMLHeadingElement() override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

private:
    HTMLHeadingElement(DOM::Document&, DOM::QualifiedName);
};

}
