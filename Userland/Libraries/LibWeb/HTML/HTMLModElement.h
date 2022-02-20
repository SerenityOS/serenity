/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLModElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLModElementWrapper;

    HTMLModElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLModElement() override;
};

}
