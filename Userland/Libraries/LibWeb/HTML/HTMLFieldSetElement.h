/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
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

    DeprecatedString const& type() const
    {
        static DeprecatedString fieldset = "fieldset";
        return fieldset;
    }

    bool is_disabled() const;

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::group; }

private:
    HTMLFieldSetElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
