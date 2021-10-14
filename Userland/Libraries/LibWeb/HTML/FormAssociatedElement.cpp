/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>

namespace Web::HTML {

void FormAssociatedElement::set_form(HTMLFormElement* form)
{
    if (m_form)
        m_form->remove_associated_element({}, *this);
    m_form = form;
    if (m_form)
        m_form->add_associated_element({}, *this);
}

}
