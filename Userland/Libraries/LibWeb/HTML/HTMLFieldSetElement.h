/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLFieldSetElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLFieldSetElementWrapper;

    HTMLFieldSetElement(DOM::Document&, QualifiedName);
    virtual ~HTMLFieldSetElement() override;

    String const& type() const
    {
        static String fieldset = "fieldset";
        return fieldset;
    }
};

}
