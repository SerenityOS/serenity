/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLOptionElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLOptionElement, HTMLElement);

public:
    virtual ~HTMLOptionElement() override;

    bool selected() const { return m_selected; }
    void set_selected(bool);

    DeprecatedString value() const;
    WebIDL::ExceptionOr<void> set_value(DeprecatedString);

    DeprecatedString text() const;
    void set_text(DeprecatedString);

    int index() const;

    bool disabled() const;

    virtual Optional<ARIA::Role> default_role() const override;

private:
    friend class Bindings::OptionConstructor;
    friend class HTMLSelectElement;

    HTMLOptionElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    void ask_for_a_reset();

    // https://html.spec.whatwg.org/multipage/form-elements.html#concept-option-selectedness
    bool m_selected { false };

    // https://html.spec.whatwg.org/multipage/form-elements.html#concept-option-dirtiness
    bool m_dirty { false };
};

}
