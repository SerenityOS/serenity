/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Iterator.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibWayland/Interface.h>

namespace Wayland {
class Object;

enum class MessageArgType {
    Primitive,
    BufferWithLength,
    Fd,
};

struct MessageArg {
    MessageArgType common_type;
    struct Argument* type;

    Variant<uint32_t, ReadonlyBytes> data;
};

class MessageOutgoing {
public:
    MessageOutgoing(uint32_t object_id, uint8_t opcode, Vector<NonnullOwnPtr<ResolvedArgument>> args)
        : m_object_id(object_id)
        , m_args(move(args))
        , m_opcode(opcode)
    {
        for (auto const& arg : m_args) {
            auto* buffer = arg->message_bytes();
            m_raw.append(buffer->bytes());
        }
        m_message_length = (uint16_t)m_raw.size() + 8;
    }

    ByteBuffer* serialize()
    {
        ByteBuffer* out = new ByteBuffer();

        bool debug = true;
        if (debug) {
            warnln("outgoing: obj({}), opcode({}), args_length({})", m_object_id, m_opcode, m_message_length);
        }
        out->append(&m_object_id, sizeof(uint32_t));
        out->append(&m_opcode, sizeof(uint16_t));
        out->append(&m_message_length, sizeof(uint16_t));

        out->append(m_raw);

        return out;
    }

    Vector<int> fds()
    {
        Vector<int> fds;

        for (auto& arg : m_args) {
            if (arg->is_fd()) {
                fds.append(arg->as_fd());
            }
        }

        return fds;
    }

private:
    uint32_t m_object_id;
    Vector<NonnullOwnPtr<ResolvedArgument>> m_args;

    uint16_t m_opcode;
    uint16_t m_message_length;

    ByteBuffer m_raw;
};

class MessageIncoming {
public:
    MessageIncoming(RefPtr<Object> object, uint32_t length_and_opcode, struct Method** methods)
        : m_object(move(object))
        , m_length_and_opcode(length_and_opcode)
    {
        if (methods != nullptr) {
            m_method = methods[opcode()];
            VERIFY(m_method != nullptr);
        }
    }
    // How to use:
    // 1. new MessageIncoming()
    // 2. amount_of_args_bytes()
    // 3. input into -> deserialize_args()
    // 4. push_resolved_args
    // 5. populate with fds
    // 6. if is_resolved: submit to object

    bool is_resolved()
    {
        if (m_method) {
            bool have_all_args = m_method->amount_args == m_resolved.size();
            bool have_all_fds = amount_unresolved_fds() == 0;
            return have_all_args && have_all_fds;
        }
        return false;
    }

    // the message is always padded to 32bits, so just using this format
    size_t amount_of_args() const
    {
        return amount_of_args_bytes() / sizeof(uint32_t);
    }

    size_t amount_of_args_bytes() const
    {
        return m_message_length - 8;
    }

    Vector<MessageArg> deserialize_args(ReadonlyBytes bytes)
    {
        VERIFY(m_method != nullptr);
        VERIFY(bytes.size() == amount_of_args_bytes());

        Vector<MessageArg> args;

        auto* arg = m_method->arg;
        auto bytes_iter = bytes.begin();

        VERIFY(arg != nullptr);
        while (*arg != nullptr) {
            auto* parg = *arg;

            MessageArgType state;

            switch (parg->type.kind) {
            case WireArgumentType::NewId:
            case WireArgumentType::Object:
            case WireArgumentType::Integer:
            case WireArgumentType::UnsignedInteger:
            case WireArgumentType::FixedFloat:
                state = MessageArgType::Primitive;
                break;
            case WireArgumentType::String:
            case WireArgumentType::Array:
                state = MessageArgType::BufferWithLength;
                break;
            case WireArgumentType::FileDescriptor:
                state = MessageArgType::Fd;
                break;
            }
            union {
                uint32_t data;
                uint8_t data_bytes[sizeof(uint32_t)];
            };

            for (size_t idx = 0; idx < sizeof(uint32_t); ++idx) {
                data_bytes[idx] = *bytes_iter;
                bytes_iter++;
            }

            ReadonlyBytes slice;

            switch (state) {
            case MessageArgType::Primitive:
                args.append(MessageArg {
                    .common_type = state,
                    .type = parg,
                    .data = data,
                });
                break;
            case MessageArgType::BufferWithLength: {
                slice = bytes.slice(bytes_iter.index(), data);
                args.append(MessageArg {
                    .common_type = state,
                    .type = parg,
                    .data = slice,
                });
                size_t to_pad = data % sizeof(uint32_t);
                if (to_pad > 0) {
                    data += sizeof(uint32_t) - to_pad;
                }
                while (data > 0) {
                    ++bytes_iter;
                    --data;
                }

                break;
            }
            case MessageArgType::Fd:
                args.append(MessageArg {
                    .common_type = state,
                    .type = parg,
                    .data = 0,
                });
                break;
            }

            arg++;
        }

        return args;
    }

    void push_resolved_args(Vector<NonnullOwnPtr<ResolvedArgument>> resolved)
    {
        m_resolved = move(resolved);
    }

    AK::SimpleIterator<Vector<NonnullOwnPtr<ResolvedArgument>>, NonnullOwnPtr<ResolvedArgument>> unresolved_fds()
    {
        auto iter = m_resolved.find_if([](NonnullOwnPtr<ResolvedArgument> const& entry) {
            return entry->is_fd() && !entry->is_fd_resolved();
        });

        return iter;
    }

    size_t amount_unresolved_fds();

    void submit();

private:
    RefPtr<Object> m_object;
    union {
        struct {
            uint16_t m_opcode;
            uint16_t m_message_length;
        };
        uint32_t m_length_and_opcode;
    };

    uint16_t opcode() const
    {
        return m_opcode;
    }

    Method* m_method { nullptr };
    ByteBuffer m_raw;

    Vector<NonnullOwnPtr<ResolvedArgument>> m_resolved;
};

}
