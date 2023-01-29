/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDetailsElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDetailsElement, HTMLElement);

public:
    virtual ~HTMLDetailsElement() override;

    // https://www.w3.org/TR/html-aria/#el-details
    virtual Optional<DOM::ARIARoles::Role> default_role() const override { return DOM::ARIARoles::Role::group; };

private:
    HTMLDetailsElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
