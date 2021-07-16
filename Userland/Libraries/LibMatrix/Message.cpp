/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/OwnPtr.h>
#include <LibMatrix/Message.h>

namespace Matrix {

OwnPtr<Message> Message::create_from_json(JsonObject const& object)
{
    auto metadata = EventMetadata::create_from_json(object);
    if (!metadata.has_value())
        return {};

    if (!object.has("content"))
        return {};
    auto content = object.get("content");
    if (!content.is_object() || !content.as_object().has("msgtype") || !content.as_object().get("msgtype").is_string())
        return {};
    auto msgtype = content.as_object().get("msgtype").as_string();

    if (msgtype == "m.text" || msgtype == "m.notice") {
        if (!content.as_object().has("body") || !content.as_object().get("body").is_string())
            return {};
        auto body = content.as_object().get("body").as_string();
        auto type = (msgtype == "m.text") ? Type::Text : Type::Notice;
        return make<TextMessage>(*metadata, type, move(body));
    }

    dbgln("[Matrix] Unimplemented 'msgtype' for 'm.room.message': '{}'", msgtype);
    return {};
}

}
