/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/CDATASection.h>
#include <LibWeb/HTML/Window.h>

namespace Web::DOM {

CDATASection::CDATASection(Document& document, String const& data)
    : Text(document, NodeType::CDATA_SECTION_NODE, data)
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "CDATASection"));
}

CDATASection::~CDATASection() = default;

}
