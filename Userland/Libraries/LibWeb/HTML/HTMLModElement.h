/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLModElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLModElement, HTMLElement);

public:
    virtual ~HTMLModElement() override;

    virtual Optional<DOM::ARIARoles::Role> default_role() const override;

private:
    HTMLModElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
