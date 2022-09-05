/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLFieldSetElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLFieldSetElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLFieldSetElement)

public:
    virtual ~HTMLFieldSetElement() override;

    String const& type() const
    {
        static String fieldset = "fieldset";
        return fieldset;
    }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

private:
    HTMLFieldSetElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLFieldSetElement, Web::HTML)
