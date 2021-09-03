/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/WeakPtr.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class FormAssociatedElement {
public:
    HTMLFormElement* form() { return m_form; }
    HTMLFormElement const* form() const { return m_form; }

    void set_form(HTMLFormElement*);

protected:
    FormAssociatedElement() = default;
    virtual ~FormAssociatedElement() = default;

    virtual HTMLElement& form_associated_element_to_html_element() = 0;

private:
    WeakPtr<HTMLFormElement> m_form;
};

}
