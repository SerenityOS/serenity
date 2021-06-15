/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMatrix/StateEvent.h>

namespace Matrix {

Optional<StateEvent> StateEvent::create_from_json(JsonObject const& object)
{
    auto metadata = EventMetadata::create_from_json(object);
    if (!metadata.has_value())
        return {};
    if (!object.has("state_key") || !object.has("content"))
        return {};
    auto state_key = object.get("state_key");
    auto content = object.get("content");
    if (!state_key.is_string() || !content.is_object())
        return {};
    return StateEvent { metadata.release_value(), state_key.as_string(), content.as_object() };
}

}
