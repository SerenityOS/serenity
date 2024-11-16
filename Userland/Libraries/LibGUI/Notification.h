/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/EventReceiver.h>
#include <LibGfx/Bitmap.h>
#include <LibURL/URL.h>

namespace GUI {

class ConnectionToNotificationServer;

class Notification : public Core::EventReceiver {
    C_OBJECT(Notification);

    friend class ConnectionToNotificationServer;

public:
    virtual ~Notification() override;

    String const& text() const { return m_text; }
    void set_text(String const& text)
    {
        m_text_dirty = true;
        m_text = text;
    }

    String const& title() const { return m_title; }
    void set_title(String const& title)
    {
        m_title_dirty = true;
        m_title = title;
    }

    Gfx::Bitmap const* icon() const { return m_icon; }
    void set_icon(Gfx::Bitmap const* icon)
    {
        m_icon_dirty = true;
        m_icon = icon;
    }

    URL::URL const& launch_url() const { return m_launch_url; }
    void set_launch_url(URL::URL const& launch_url)
    {
        m_launch_url_dirty = true;
        m_launch_url = launch_url;
    }

    void show();
    bool update();
    void close();

    bool is_showing() const { return m_shown && !m_destroyed; }

private:
    Notification();

    void connection_closed();

    String m_title;
    bool m_title_dirty { false };
    String m_text;
    bool m_text_dirty { false };
    RefPtr<Gfx::Bitmap const> m_icon;
    bool m_icon_dirty { false };
    URL::URL m_launch_url;
    bool m_launch_url_dirty { false };

    bool m_destroyed { false };
    bool m_shown { false };
    RefPtr<ConnectionToNotificationServer> m_connection;
};

}
