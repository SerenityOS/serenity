/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Text.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Text::Text(Document& document, String const& data)
    : CharacterData(document, NodeType::TEXT_NODE, data)
{
}

Text::~Text()
{
}

RefPtr<Layout::Node> Text::create_layout_node()
{
    return adopt_ref(*new Layout::TextNode(document(), *this));
}

}
