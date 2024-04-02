/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/NeverDestroyed.h>
#include <AK/Try.h>
#include <LibCore/Environment.h>
#include <LibWayland/Connection.h>
#include <LibWayland/wayland-protocol.h>

namespace Wayland {

ErrorOr<NonnullOwnPtr<Connection>> Connection::open()
{
    auto result = Core::Environment::get("XDG_RUNTIME_DIR"sv);
    if (!result.has_value()) {
        return Error::from_string_literal("LibWayland requires XDG_RUNTIME_DIR to be set. Aborting.");
    }
    auto xdg_runtime_dir_path = result.release_value();

    // We default to wayland-0 if unset, just like libwayland-client
    auto wayland_display = Core::Environment::get("WAYLAND_DISPLAY"sv).value_or_lazy_evaluated([] {
        return "wayland-0"sv;
    });

    auto path = LexicalPath::join(xdg_runtime_dir_path, wayland_display);

    return open_with_path(path.string());
}

ErrorOr<NonnullOwnPtr<Connection>> Connection::open_with_path(ByteString const& path)
{
    auto socket = TRY(Core::LocalSocket::connect(path));

    return open_socket(move(socket));
}

ErrorOr<NonnullOwnPtr<Connection>> Connection::open_socket(NonnullOwnPtr<Core::LocalSocket> socket)
{
    VERIFY(socket->is_open());

    TRY(socket->set_close_on_exec(true));
    MUST(socket->set_blocking(true));

    auto instance = adopt_own(*new Connection(move(socket)));
    return instance;
}

Display& Connection::get_display()
{
    auto object = get_object_by_id(1);
    VERIFY(!object.is_null());

    return static_cast<Display&>(*object);
}

RefPtr<Object> Connection::get_object_by_id(uint32_t id)
{
    if (id == 0) {
        return nullptr;
    }

    auto it = m_objects_map.find(id);
    if (it != m_objects_map.end())
        return (*it).value;

    return nullptr;
}

ErrorOr<void> Connection::send_fds()
{

    for (size_t idx = 0; idx < m_fds_to_send.size();) {
        int fd = m_fds_to_send.at(idx);
        auto result = m_socket->send_fd(fd);
        if (result.is_error()) {
            auto error = result.release_error();
            if (error.code() == EBUSY) {
                return {};
            }
            return error;
        }
        m_fds_to_send.remove(idx);
    }

    return {};
}

ErrorOr<void> Connection::write()
{
    // Only write to socket, if there are no messages in the incoming event queue.
    if (m_queue_incoming.size() > 0) {
        warnln("have incoming");
        return {};
    }

    TRY(send_fds());

    if (m_queue_outgoing.is_empty()) {
        warnln("no outgoing");
        return {};
    }

    for (size_t i = 0; i < m_queue_outgoing.size();) {
        auto& msg = m_queue_outgoing.at(i);
        auto* buffer = msg->serialize();
        auto bytes = buffer->bytes();
        (void)TRY(m_socket->write_some(bytes));
        m_fds_to_send.extend(msg->fds());
        m_queue_outgoing.remove(i);
        delete buffer;
    }

    TRY(send_fds());

    return {};
}

ErrorOr<void> Connection::read()
{
    // we read as much as possible into a buffer first, then parse the messages in the buffer
    union {
        struct {
            uint32_t object_id;
            union {
                struct {
                    uint16_t opcode;
                    uint16_t args_length;
                };
                uint32_t second_arg;
            };
        } raw_message_header;
        uint8_t raw_bytes[sizeof(raw_message_header)];
    };

    auto buffer
        = MUST(ByteBuffer::create_uninitialized(4096));

    ByteBuffer unprocessed_bytes;
    {
        Bytes result = TRY(m_socket->read_some(buffer));
        unprocessed_bytes.append(m_unprocessed_bytes);
        unprocessed_bytes.append(result);
        m_unprocessed_bytes.clear();
    }

    while (unprocessed_bytes.size() > 0) {
        if (unprocessed_bytes.size() < sizeof(raw_message_header)) {
            m_unprocessed_bytes.append(unprocessed_bytes);
            warnln("not enough bytes");
            return Error::from_errno(EAGAIN);
        }

        // if new message
        for (size_t i = 0; i < sizeof(raw_message_header); i++) {
            raw_bytes[i] = unprocessed_bytes.bytes().at(i);
        }

        auto obj = get_object_by_id(raw_message_header.object_id);
        auto** methods = !obj.is_null() ? obj->interface().events : nullptr;
        auto incoming_msg = make<MessageIncoming>(obj, raw_message_header.second_arg, methods);

        // FIXME
        // if we don't have enough bytes to fill the message, then we store it for later and discard the MsgIncoming object
        if (incoming_msg->amount_of_args_bytes() > unprocessed_bytes.bytes().size() - sizeof(raw_message_header)) {
            m_unprocessed_bytes.append(unprocessed_bytes);
            warnln("not enough argument bytes, {} {}", incoming_msg->amount_of_args_bytes(), unprocessed_bytes.bytes().size());
            return Error::from_errno(EAGAIN);
        }
        auto args_buffer = MUST(unprocessed_bytes.slice(sizeof(raw_message_header), incoming_msg->amount_of_args_bytes()));
        auto args = incoming_msg->deserialize_args(args_buffer.bytes());
        {
            ByteBuffer new_unprocessed_bytes;
            auto new_offset = sizeof(raw_message_header) + incoming_msg->amount_of_args_bytes();
            new_unprocessed_bytes.append(MUST(unprocessed_bytes.slice(new_offset, unprocessed_bytes.size() - new_offset)));
            unprocessed_bytes = new_unprocessed_bytes;
        }

        Vector<NonnullOwnPtr<ResolvedArgument>> args_resolved;
        for (auto& arg : args) {
            switch (arg.type->type.kind) {
            case WireArgumentType::Integer:
            case WireArgumentType::UnsignedInteger:
            case WireArgumentType::FixedFloat:
            case WireArgumentType::NewId: {
                args_resolved.append(make<ResolvedArgument>(arg.type, arg.data.get<uint32_t>()));
                break;
            }
            case WireArgumentType::Object: {
                args_resolved.append(make<ResolvedArgument>(arg.type, get_object_by_id(arg.data.get<uint32_t>())));
                break;
            }
            case WireArgumentType::String: {
                auto& data = arg.data.get<ReadonlyBytes>();
                if (data.is_empty()) {
                    VERIFY(arg.type->type.nullable);
                    args_resolved.append(make<ResolvedArgument>(arg.type, Optional<ByteString>()));
                    break;
                }
                // FIXME
                ByteString string = ByteString::formatted("{}", (char const*)data.data());
                VERIFY(string != nullptr);
                if (arg.type->type.nullable) {
                    args_resolved.append(make<ResolvedArgument>(arg.type, Optional<ByteString>(string)));
                } else {
                    args_resolved.append(make<ResolvedArgument>(arg.type, string));
                }
                break;
            }
            case WireArgumentType::Array: {
                ByteBuffer b;
                b.append(arg.data.get<ReadonlyBytes>());
                args_resolved.append(make<ResolvedArgument>(arg.type, b));
                break;
            }
            case WireArgumentType::FileDescriptor:
                args_resolved.append(make<ResolvedArgument>(arg.type, 0));
                break;
            }
        }

        incoming_msg->push_resolved_args(move(args_resolved));
        m_queue_incoming.append(move(incoming_msg));
    }

    for (size_t i = 0; i < m_queue_incoming.size();) {
        auto& msg = m_queue_incoming.at(i);

        if (msg->is_resolved()) {
            msg->submit();
            m_queue_incoming.remove(i);
        } else {
            auto amount = msg->amount_unresolved_fds();
            VERIFY(amount > 0);

            while (amount > 0) {
                auto result = TRY(m_socket->receive_fd(0));
                msg->unresolved_fds()->ptr()->push_fd(result);
                --amount;
            }
        }
    }

    return {};
}

Connection::Connection(NonnullOwnPtr<Core::LocalSocket> socket)
    : m_socket(move(socket))
    , m_id_allocator(2, static_cast<int>(MAX_CLIENT_ID), AK::IDAllocatorMode::Increasing, AK::IDAllocatorTypeMode::Unsigned)
{
    // We setup our own notifiers, as we also want to listen on error/hangups
    auto wayland_fd = m_socket->fd().value();
    m_notifier_read = Core::Notifier::construct(wayland_fd, Core::Notifier::Type::Read);
    m_notifier_read->set_enabled(true);
    m_notifier_read->on_activation = [this] {
        // We just try to read to our event queue.
        (void)this->read();
    };

    m_notifier_error = Core::Notifier::construct(wayland_fd, Core::Notifier::Type::Error | Core::Notifier::Type::HangUp);
    m_notifier_error->set_enabled(true);
    m_notifier_error->on_activation = [this] {
        // We are supposed to send more data.
        (void)this->write();
    };

    // Declared in the spec
    (void)make_object_foreign_id<Display>(1);
}
}
