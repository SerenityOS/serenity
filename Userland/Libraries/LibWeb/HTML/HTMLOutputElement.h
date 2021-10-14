/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>

namespace Web::HTML {

class HTMLOutputElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLOutputElementWrapper;

    HTMLOutputElement(DOM::Document&, QualifiedName);
    virtual ~HTMLOutputElement() override;

    const String& type() const
    {
        static String output = "output";
        return output;
    }
};

}
