/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHRElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLHRElement, HTMLElement);

public:
    virtual ~HTMLHRElement() override;

    // https://www.w3.org/TR/html-aria/#el-hr
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::separator; }

private:
    HTMLHRElement(DOM::Document&, DOM::QualifiedName);
};

}
