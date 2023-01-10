/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLFontElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLFontElement, HTMLElement);

public:
    virtual ~HTMLFontElement() override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

private:
    HTMLFontElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
