/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLOptGroupElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLOptGroupElement, HTMLElement);

public:
    virtual ~HTMLOptGroupElement() override;

    // https://www.w3.org/TR/html-aria/#el-optgroup
    virtual FlyString default_role() const override { return DOM::ARIARoleNames::group; }

private:
    HTMLOptGroupElement(DOM::Document&, DOM::QualifiedName);
};

}
