/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLOListElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLOListElement, HTMLElement);

public:
    virtual ~HTMLOListElement() override;

    // https://www.w3.org/TR/html-aria/#el-ol
    virtual Optional<DOM::ARIARoles::Role> default_role() const override { return DOM::ARIARoles::Role::list; }

private:
    HTMLOListElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
