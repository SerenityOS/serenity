/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMenuElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMenuElement, HTMLElement);

public:
    virtual ~HTMLMenuElement() override;

    // https://www.w3.org/TR/html-aria/#el-menu
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::list; }

private:
    HTMLMenuElement(DOM::Document&, DOM::QualifiedName);
};

}
