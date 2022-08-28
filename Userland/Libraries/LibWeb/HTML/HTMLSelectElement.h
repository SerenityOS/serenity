/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLOptionsCollection.h>

namespace Web::HTML {

class HTMLSelectElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLSelectElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLSelectElement)

public:
    virtual ~HTMLSelectElement() override;

    RefPtr<HTMLOptionsCollection> const& options();

    DOM::ExceptionOr<void> add(HTMLOptionOrOptGroupElement element, Optional<HTMLElementOrElementIndex> before = {});

    int selected_index() const;
    void set_selected_index(int);

    Vector<JS::Handle<HTMLOptionElement>> list_of_options() const;

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-select-element
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
    HTMLSelectElement(DOM::Document&, DOM::QualifiedName);

    RefPtr<HTMLOptionsCollection> m_options;
};

}

WRAPPER_HACK(HTMLSelectElement, Web::HTML)
