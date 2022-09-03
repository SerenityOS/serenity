/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeList.h>
#include <LibWeb/HTML/Window.h>

namespace Web::DOM {

NodeList::NodeList(HTML::Window& window)
    : LegacyPlatformObject(window.cached_web_prototype("NodeList"))
{
}

NodeList::~NodeList() = default;

JS::Value NodeList::item_value(size_t index) const
{
    auto* node = item(index);
    if (!node)
        return JS::js_undefined();
    return const_cast<Node*>(node);
}

bool NodeList::is_supported_property_index(u32 index) const
{
    return index < length();
}

}
