/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Try.h>
#include <AK/NeverDestroyed.h>
#include <LibCore/Environment.h>
#include <LibWayland/Connection.h>

namespace Wayland {

ErrorOr<Connection> Connection::open()
{
    auto xdg_runtime_dir_path = TRY(Core::Environment::get("XDG_RUNTIME_DIR"sv).try_value_or_lazy_evaluated([] {
        return Error::from_string_literal("LibWayland requires XDG_RUNTIME_DIR to be set. Aborting.");
    }));

    // We default to wayland-0 if unset, just like libwayland-client
    auto wayland_display = Core::Environment::get("WAYLAND_DISPLAY"sv).value_or_lazy_evaluated([] {
        return "wayland-0"sv;
    });

    auto path = LexicalPath::join(xdg_runtime_dir_path, wayland_display);

    return open_with_path(path.string());
}

ErrorOr<Connection> Connection::open_with_path(ByteString path)
{
    auto socket = TRY(Core::LocalSocket::connect(path));

    return Connection(move(socket));
}

// Display& Connection::get_display() {
//     auto value = static_cast<Display*>(get_object_by_id(1));

//     return &value;
// }

Object* Connection::get_object_by_id(uint32_t id)
{
    auto it = m_objects_map.find(id);
    if (it != m_objects_map.end())
        return (*it).value;

    return nullptr;
}

Connection::Connection(NonnullOwnPtr<Core::LocalSocket> socket)
    : m_socket(move(socket))
{
    // We setup our own notifiers, as we also want to listen on error/hangups
    auto wayland_fd = m_socket->fd().release_value();
    m_notifier_read = Core::Notifier::construct(wayland_fd, Core::Notifier::Type::Read);
    m_notifier_read->set_enabled(true);
    m_notifier_read->on_activation = [this]{
        // We can read to our event queue now.
        this->read();
    };

    m_notifier_error = Core::Notifier::construct(wayland_fd, Core::Notifier::Type::Error | Core::Notifier::Type::HangUp);
    m_notifier_error->set_enabled(true);
    m_notifier_error->on_activation = [this] {
        // We are supposed to send more data.
        this->write();
    };
}

}
