/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Notification.h>
#include <LibIPC/ServerConnection.h>
#include <NotificationServer/NotificationClientEndpoint.h>
#include <NotificationServer/NotificationServerEndpoint.h>

namespace GUI {

class NotificationServerConnection : public IPC::ServerConnection<NotificationClientEndpoint, NotificationServerEndpoint>
    , public NotificationClientEndpoint {
    C_OBJECT(NotificationServerConnection)

    friend class Notification;

public:
    virtual void handshake() override
    {
        send_sync<Messages::NotificationServer::Greet>();
    }

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
    virtual void handle(const Messages::NotificationClient::Dummy&) override { }
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
    m_connection->send_sync<Messages::NotificationServer::ShowNotification>(m_text, m_title, icon);
    m_shown = true;
}

void Notification::close()
{
    VERIFY(m_shown);
    if (!m_destroyed) {
        m_connection->send_sync<Messages::NotificationServer::CloseNotification>();
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
        m_connection->send_sync<Messages::NotificationServer::UpdateNotificationText>(m_text, m_title);
        m_text_dirty = false;
        m_title_dirty = false;
    }

    if (m_icon_dirty) {
        m_connection->send_sync<Messages::NotificationServer::UpdateNotificationIcon>(m_icon ? m_icon->to_shareable_bitmap() : Gfx::ShareableBitmap());
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
