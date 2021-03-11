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
public:
    virtual void handshake() override
    {
        send_sync<Messages::NotificationServer::Greet>();
    }

    virtual void die() override { m_connected = false; }

    bool is_connected() const { return m_connected; }

private:
    NotificationServerConnection()
        : IPC::ServerConnection<NotificationClientEndpoint, NotificationServerEndpoint>(*this, "/tmp/portal/notify")
    {
    }
    virtual void handle(const Messages::NotificationClient::Dummy&) override { }
    bool m_connected { true };
};

Notification::Notification()
    : m_connection(NotificationServerConnection::construct())
{
}

Notification::~Notification()
{
}

void Notification::show()
{
    VERIFY(!m_showing);
    VERIFY(!m_disposed);
    if (!m_connection->is_connected()) {
        // This would imply that the NotificationServer crashed before we could send it any data.
        VERIFY_NOT_REACHED();
    }
    auto icon = m_icon ? m_icon->to_shareable_bitmap() : Gfx::ShareableBitmap();
    m_connection->send_sync<Messages::NotificationServer::ShowNotification>(m_text, m_title, icon);
    m_showing = true;
}

void Notification::close()
{
    VERIFY(m_showing);
    if (m_connection->is_connected()) {
        m_connection->send_sync<Messages::NotificationServer::CloseNotification>();
    }

    m_showing = false;
    m_disposed = true;
}

bool Notification::update()
{
    VERIFY(m_showing);
    VERIFY(!m_disposed);
    if (!m_connection->is_connected()) {
        m_showing = false;
        m_disposed = true;
        return false;
    }

    bool is_checked = false;

    if (m_text_dirty || m_title_dirty) {
        auto response = m_connection->send_sync<Messages::NotificationServer::UpdateNotificationText>(m_text, m_title);
        m_text_dirty = false;
        m_title_dirty = false;

        is_checked = true;
        if (!response->still_showing()) {
            m_showing = false;
            m_disposed = true;
            return false;
        }
    }

    if (m_icon_dirty) {
        auto response = m_connection->send_sync<Messages::NotificationServer::UpdateNotificationIcon>(m_icon ? m_icon->to_shareable_bitmap() : Gfx::ShareableBitmap());
        m_icon_dirty = false;

        is_checked = true;
        if (!response->still_showing()) {
            m_showing = false;
            m_disposed = true;
            return false;
        }
    }

    if (!is_checked) {
        auto response = m_connection->send_sync<Messages::NotificationServer::IsShowing>();
        m_showing = response->still_showing();
        if (!m_showing) {
            m_disposed = true;
        }
    }
    return m_showing;
}

}
