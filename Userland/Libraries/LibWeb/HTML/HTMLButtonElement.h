/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

#define ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTES              \
    __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(submit, Submit) \
    __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(reset, Reset)   \
    __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(button, Button)

class HTMLButtonElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLButtonElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLButtonElement)

public:
    virtual ~HTMLButtonElement() override;

    enum class TypeAttributeState {
#define __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE(_, state) state,
        ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_BUTTON_TYPE_ATTRIBUTE
    };

    String type() const;
    TypeAttributeState type_state() const;
    void set_type(String const&);

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-button-element
    virtual bool is_focusable() const override { return true; }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-submit
    virtual bool is_submittable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

private:
    HTMLButtonElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLButtonElement, Web::HTML)
