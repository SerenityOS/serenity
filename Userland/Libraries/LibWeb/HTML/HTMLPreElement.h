/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLPreElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLPreElement, HTMLElement);

public:
    virtual ~HTMLPreElement() override;

    // https://www.w3.org/TR/html-aria/#el-pre
    virtual Optional<DOM::ARIARoles::Role> default_role() const override { return DOM::ARIARoles::Role::generic; }

private:
    HTMLPreElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
