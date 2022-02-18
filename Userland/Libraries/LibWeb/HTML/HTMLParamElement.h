/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLParamElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLParamElementWrapper;

    HTMLParamElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLParamElement() override;
};

}
