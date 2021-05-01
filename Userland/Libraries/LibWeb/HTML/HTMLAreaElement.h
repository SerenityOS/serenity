/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLAreaElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLAreaElementWrapper;

    HTMLAreaElement(DOM::Document&, QualifiedName);
    virtual ~HTMLAreaElement() override;
};

}
