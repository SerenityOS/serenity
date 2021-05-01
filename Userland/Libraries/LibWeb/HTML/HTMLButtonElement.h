/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLButtonElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLButtonElementWrapper;

    HTMLButtonElement(DOM::Document&, QualifiedName);
    virtual ~HTMLButtonElement() override;
};

}
