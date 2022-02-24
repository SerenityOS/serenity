/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDivElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLDivElementWrapper;

    HTMLDivElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLDivElement() override;
};

}
