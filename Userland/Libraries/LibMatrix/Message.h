/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibMatrix/EventMetadata.h>
#include <LibMatrix/Id.h>

namespace Matrix {

class Message {
public:
    enum class Type {
        Text,
        Emote,
        Notice,
        Image,
        File,
        Audio,
        Location,
        Video,
        State
    };

    static OwnPtr<Message> create_from_json(JsonObject const&);

    EventMetadata const& metadata() const { return m_metadata; }
    Type type() const { return m_type; }

    virtual ~Message() = default;

protected:
    Message(EventMetadata metadata, Type type)
        : m_metadata(move(metadata))
        , m_type(type)
    {
    }

    EventMetadata m_metadata;
    Type m_type;
};

class TextMessage : public Message {
public:
    TextMessage(EventMetadata metadata, Type type, String body)
        : Message(move(metadata), type)
        , m_body(move(body))
    {
        VERIFY(type == Type::Text || type == Type::Notice);
    }

    String const& body() const { return m_body; }

private:
    String m_body;
};

}
