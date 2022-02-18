/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/WeakPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class FormAssociatedElement : public HTMLElement {
public:
    HTMLFormElement* form() { return m_form; }
    HTMLFormElement const* form() const { return m_form; }

    void set_form(HTMLFormElement*);

protected:
    FormAssociatedElement(DOM::Document& document, DOM::QualifiedName qualified_name)
        : HTMLElement(document, move(qualified_name))
    {
    }

    virtual ~FormAssociatedElement() = default;

private:
    WeakPtr<HTMLFormElement> m_form;
};

}
