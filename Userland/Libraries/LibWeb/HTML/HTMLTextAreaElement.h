/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTextAreaElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLTextAreaElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLTextAreaElement)

public:
    virtual ~HTMLTextAreaElement() override;

    String const& type() const
    {
        static String textarea = "textarea";
        return textarea;
    }

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-textarea-element
    virtual bool is_focusable() const override { return true; }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-submit
    virtual bool is_submittable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-reset
    virtual bool is_resettable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

private:
    HTMLTextAreaElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLTextAreaElement, Web::HTML)
