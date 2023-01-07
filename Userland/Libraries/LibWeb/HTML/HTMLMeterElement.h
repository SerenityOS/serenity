/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMeterElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMeterElement, HTMLElement);

public:
    virtual ~HTMLMeterElement() override;

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

    // https://www.w3.org/TR/html-aria/#el-meter
    virtual FlyString default_role() const override { return DOM::ARIARoleNames::meter; }

private:
    HTMLMeterElement(DOM::Document&, DOM::QualifiedName);
};

}
