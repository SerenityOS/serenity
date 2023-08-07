/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLLegendElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLLegendElement, HTMLElement);

public:
    virtual ~HTMLLegendElement() override;

private:
    HTMLLegendElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
