/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Notification.h>
#include <LibIPC/ConnectionToServer.h>
#include <NotificationServer/NotificationClientEndpoint.h>
#include <NotificationServer/NotificationServerEndpoint.h>

namespace GUI {

class ConnectionToNotificationServer final
    : public IPC::ConnectionToServer<NotificationClientEndpoint, NotificationServerEndpoint>
    , public NotificationClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToNotificationServer, "/tmp/session/%sid/portal/notify"sv)

    friend class Notification;

public:
    virtual void die() override
    {
        if (!m_notification->m_destroyed)
            m_notification->connection_closed();
    }

private:
    explicit ConnectionToNotificationServer(NonnullOwnPtr<Core::LocalSocket> socket, Notification* notification)
        : IPC::ConnectionToServer<NotificationClientEndpoint, NotificationServerEndpoint>(*this, move(socket))
        , m_notification(notification)
    {
    }
    Notification* m_notification;
};

Notification::Notification() = default;
Notification::~Notification() = default;

void Notification::show()
{
    VERIFY(!m_shown && !m_destroyed);
    auto icon = m_icon ? m_icon->to_shareable_bitmap() : Gfx::ShareableBitmap();
    m_connection = ConnectionToNotificationServer::try_create(this).release_value_but_fixme_should_propagate_errors();
    m_connection->show_notification(m_text, m_title, icon, m_launch_url);
    m_shown = true;
}

void Notification::close()
{
    VERIFY(m_shown);
    if (!m_destroyed) {
        m_connection->close_notification();
        connection_closed();
        return;
    }
}

bool Notification::update()
{
    VERIFY(m_shown);
    if (m_destroyed) {
        return false;
    }

    if (m_text_dirty || m_title_dirty) {
        m_connection->update_notification_text(m_text, m_title);
        m_text_dirty = false;
        m_title_dirty = false;
    }

    if (m_icon_dirty) {
        m_connection->update_notification_icon(m_icon ? m_icon->to_shareable_bitmap() : Gfx::ShareableBitmap());
        m_icon_dirty = false;
    }

    if (m_launch_url_dirty) {
        m_connection->update_notification_launch_url(m_launch_url);
        m_launch_url_dirty = false;
    }

    return true;
}

void Notification::connection_closed()
{
    m_connection.clear();
    m_destroyed = true;
}

}
