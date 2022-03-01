/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>

namespace Web::HTML {

class HTMLFieldSetElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLFieldSetElementWrapper;

    HTMLFieldSetElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLFieldSetElement() override;

    const String& type() const
    {
        static String fieldset = "fieldset";
        return fieldset;
    }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }
};

}
