/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>

namespace Wayland {
enum MessageType {
    Event,
    Request,
};

class WireArgument {
public:
    uint32_t arg;
};

class Message {
public:
    Message(MessageType type, uint32_t object_id, uint32_t length_and_opcode)
        : m_type(type)
        , m_object_id(object_id)
        , m_length_and_opcode(length_and_opcode)
    {
    }

private:
    MessageType m_type;

    uint32_t m_object_id;
    union {
        struct {
            uint16_t m_message_length;
            uint16_t m_opcode;
        };
        uint32_t m_length_and_opcode;
    };
    Vector<WireArgument> m_args;

    uint8_t opcode() const
    {
        return m_opcode;
    }

    // the message is always padded to 32bits, so just using this format
    uint32_t amount_of_args() const
    {
        return (m_message_length - 8) / 4;
    }
};

}
