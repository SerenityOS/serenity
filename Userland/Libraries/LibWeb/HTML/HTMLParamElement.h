/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLParamElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLParamElement, HTMLElement);

public:
    virtual ~HTMLParamElement() override;

private:
    HTMLParamElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
