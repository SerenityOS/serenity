/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDListElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLDListElementWrapper;

    HTMLDListElement(DOM::Document&, QualifiedName);
    virtual ~HTMLDListElement() override;
};

}
