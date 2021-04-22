/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableColElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTableColElementWrapper;

    HTMLTableColElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTableColElement() override;
};

}
