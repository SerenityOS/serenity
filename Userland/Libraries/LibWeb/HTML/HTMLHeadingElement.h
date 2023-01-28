/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHeadingElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLHeadingElement, HTMLElement);

public:
    virtual ~HTMLHeadingElement() override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    // https://www.w3.org/TR/html-aria/#el-h1-h6
    virtual Optional<DOM::ARIARoles::Role> default_role() const override { return DOM::ARIARoles::Role::heading; }

    virtual DeprecatedString aria_level() const override
    {
        // TODO: aria-level = the number in the element's tag name
        return get_attribute("aria-level");
    }

private:
    HTMLHeadingElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
