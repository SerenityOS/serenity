/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibMatrix/EventMetadata.h>

namespace Matrix {

Optional<EventMetadata> EventMetadata::create_from_json(JsonObject const& object)
{
    if (!object.has("event_id") || !object.has("type") || !object.has("sender") || !object.has("origin_server_ts"))
        return {};

    auto id = object.get("event_id");
    auto type = object.get("type");
    auto sender = object.get("sender");
    auto timestamp_in_milliseconds = object.get("origin_server_ts");

    if (!id.is_string() || !type.is_string() || !sender.is_string() || !timestamp_in_milliseconds.is_number())
        return {};

    return EventMetadata { EventId(id.as_string()), type.as_string(), UserId(sender.as_string()), timestamp_in_milliseconds.to_u64() };
}

}
