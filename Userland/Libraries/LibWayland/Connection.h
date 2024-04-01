/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IDAllocator.h>
#include <LibCore/Socket.h>
#include <LibWayland/Message.h>

namespace Wayland {

class Display;
class Object;
class MessageOutgoing;
class MessageIncoming;

class Connection : public OwnPtr<Connection> {
    friend class Object;

public:
    static ErrorOr<NonnullOwnPtr<Connection>> open();
    static ErrorOr<NonnullOwnPtr<Connection>> open_with_path(ByteString const& path);
    static ErrorOr<NonnullOwnPtr<Connection>> open_socket(NonnullOwnPtr<Core::LocalSocket> socket);

    Display& get_display();

    ErrorOr<void> read();

    ErrorOr<void> write();

    template<class A>
    NonnullRefPtr<A> make_object_own_id()
    {
        auto new_id = m_id_allocator.allocate();

        auto* a = new A(*this, new_id);
        VERIFY(a != nullptr);
        auto a_ref = adopt_ref(*a);

        m_objects_map.set(new_id, a_ref);

        return a_ref;
    }

    template<class A>
    NonnullRefPtr<A> make_object_foreign_id(uint32_t foreign_id)
    {
        auto* a = new A(*this, foreign_id);
        VERIFY(a != nullptr);
        auto a_ref = adopt_ref(*a);

        m_objects_map.set(foreign_id, a_ref);

        return a_ref;
    }

    void submit_message(NonnullOwnPtr<MessageOutgoing> msg)
    {
        m_queue_outgoing.append(move(msg));
    }

private:
    static uint const MAX_CLIENT_ID = 0xfeffffff;

    Connection(NonnullOwnPtr<Core::LocalSocket> socket);

    NonnullOwnPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Notifier> m_notifier_error;
    RefPtr<Core::Notifier> m_notifier_read;

    RefPtr<Object> get_object_by_id(uint32_t id);

    HashMap<uint32_t, NonnullRefPtr<Object>> m_objects_map;
    Vector<NonnullOwnPtr<MessageOutgoing>> m_queue_outgoing;
    Vector<int> m_fds_to_send;
    ErrorOr<void> send_fds();

    Vector<NonnullOwnPtr<MessageIncoming>> m_queue_incoming;
    ByteBuffer m_unprocessed_bytes;
    IDAllocator m_id_allocator;
};

}
