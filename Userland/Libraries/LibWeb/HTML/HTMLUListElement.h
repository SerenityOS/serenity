/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLUListElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLUListElement, HTMLElement);

public:
    virtual ~HTMLUListElement() override;

    // https://www.w3.org/TR/html-aria/#el-ul
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::list; }

private:
    HTMLUListElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
