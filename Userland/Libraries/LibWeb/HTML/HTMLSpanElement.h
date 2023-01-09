/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSpanElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLSpanElement, HTMLElement);

public:
    virtual ~HTMLSpanElement() override;

    // https://www.w3.org/TR/html-aria/#el-span
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::generic; }

private:
    HTMLSpanElement(DOM::Document&, DOM::QualifiedName);
};

}
