/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

void StyleSheet::set_owner_node(DOM::Element* element)
{
    if (element)
        m_owner_node = element->make_weak_ptr<DOM::Element>();
    else
        m_owner_node = nullptr;
}

}
