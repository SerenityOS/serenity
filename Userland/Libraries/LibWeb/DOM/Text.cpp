/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Text::Text(Document& document, const String& data)
    : CharacterData(document, NodeType::TEXT_NODE, data)
{
}

Text::~Text()
{
}

// https://dom.spec.whatwg.org/#dom-text-text
NonnullRefPtr<Text> Text::create_with_global_object(Bindings::WindowObject& window, String const& data)
{
    return make_ref_counted<Text>(window.impl().associated_document(), data);
}

}
