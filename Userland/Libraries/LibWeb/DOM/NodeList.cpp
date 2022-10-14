/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

NodeList::NodeList(JS::Realm& realm)
    : LegacyPlatformObject(Bindings::cached_web_prototype(realm, "NodeList"))
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
