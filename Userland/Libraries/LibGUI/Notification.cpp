/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Notification.h>
#include <LibIPC/ServerConnection.h>
#include <NotificationServer/NotificationClientEndpoint.h>
#include <NotificationServer/NotificationServerEndpoint.h>

namespace GUI {

class NotificationServerConnection final
    : public IPC::ServerConnection<NotificationClientEndpoint, NotificationServerEndpoint>
    , public NotificationClientEndpoint {
    C_OBJECT(NotificationServerConnection)

    friend class Notification;

public:
    virtual void die() override
    {
        m_notification->connection_closed();
    }

private:
    explicit NotificationServerConnection(Notification* notification)
        : IPC::ServerConnection<NotificationClientEndpoint, NotificationServerEndpoint>(*this, "/tmp/portal/notify")
        , m_notification(notification)
    {
    }
    virtual void dummy() override { }
    Notification* m_notification;
};

Notification::Notification()
{
}

Notification::~Notification()
{
}

void Notification::show()
{
    VERIFY(!m_shown && !m_destroyed);
    auto icon = m_icon ? m_icon->to_shareable_bitmap() : Gfx::ShareableBitmap();
    m_connection = NotificationServerConnection::construct(this);
    m_connection->show_notification(m_text, m_title, icon);
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

    return true;
}

void Notification::connection_closed()
{
    m_connection.clear();
    m_destroyed = true;
}

}
