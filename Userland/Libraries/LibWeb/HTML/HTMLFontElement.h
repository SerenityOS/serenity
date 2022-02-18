/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLFontElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLFontElementWrapper;

    HTMLFontElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLFontElement() override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
