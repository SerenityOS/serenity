/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLPreElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLPreElementWrapper;

    HTMLPreElement(DOM::Document&, QualifiedName);
    virtual ~HTMLPreElement() override;
};

}
