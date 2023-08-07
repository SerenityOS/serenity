/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLOutputElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLOutputElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLOutputElement)

public:
    virtual ~HTMLOutputElement() override;

    DeprecatedString const& type() const
    {
        static DeprecatedString output = "output";
        return output;
    }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_resettable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

    virtual void reset_algorithm() override;

    // https://www.w3.org/TR/html-aria/#el-output
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::status; }

private:
    HTMLOutputElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
