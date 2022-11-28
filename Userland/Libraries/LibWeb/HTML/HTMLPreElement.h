/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLPreElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLPreElement, HTMLElement);

public:
    virtual ~HTMLPreElement() override;

    // https://www.w3.org/TR/html-aria/#el-pre
    virtual FlyString default_role() const override { return DOM::ARIARoleNames::generic; }

private:
    HTMLPreElement(DOM::Document&, DOM::QualifiedName);
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
