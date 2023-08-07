/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSlotElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLSlotElement, HTMLElement);

public:
    virtual ~HTMLSlotElement() override;

private:
    HTMLSlotElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
