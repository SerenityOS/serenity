/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMeterElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLMeterElementWrapper;

    HTMLMeterElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLMeterElement() override;
};

}
