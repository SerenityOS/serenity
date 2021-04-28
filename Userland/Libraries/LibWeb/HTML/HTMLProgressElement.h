/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLProgressElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLProgressElementWrapper;

    HTMLProgressElement(DOM::Document&, QualifiedName);
    virtual ~HTMLProgressElement() override;
};

}
