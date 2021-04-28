/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMenuElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLMenuElementWrapper;

    HTMLMenuElement(DOM::Document&, QualifiedName);
    virtual ~HTMLMenuElement() override;
};

}
