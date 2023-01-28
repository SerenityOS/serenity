/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSpanElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLSpanElement, HTMLElement);

public:
    virtual ~HTMLSpanElement() override;

    // https://www.w3.org/TR/html-aria/#el-span
    virtual Optional<DOM::ARIARoles::Role> default_role() const override { return DOM::ARIARoles::Role::generic; }

private:
    HTMLSpanElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
