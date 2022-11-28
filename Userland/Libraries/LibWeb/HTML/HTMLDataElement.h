/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDataElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDataElement, HTMLElement);

public:
    virtual ~HTMLDataElement() override;

    // https://www.w3.org/TR/html-aria/#el-data
    virtual FlyString default_role() const override { return DOM::ARIARoleNames::generic; }

private:
    HTMLDataElement(DOM::Document&, DOM::QualifiedName);
};

}
