/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/Optional.h>
#include <LibMatrix/EventMetadata.h>
#include <LibMatrix/Id.h>

namespace Matrix {

class StateEvent {
public:
    static Optional<StateEvent> create_from_json(JsonObject const&);

    EventMetadata metadata() const { return m_metadata; }
    String const& state_key() const { return m_state_key; }
    JsonObject const& content() const { return m_content; }

protected:
    StateEvent(EventMetadata metadata, String state_key, JsonObject content)
        : m_metadata(move(metadata))
        , m_state_key(move(state_key))
        , m_content(move(content))
    {
        // NOTE: The state key is often empty, but should never be null.
        VERIFY(!m_state_key.is_null());
    }

    EventMetadata m_metadata;
    String m_state_key;
    JsonObject m_content;
};

}
