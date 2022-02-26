/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class ConnectionToNotificationServer;

class Notification : public Core::Object {
    C_OBJECT(Notification);

    friend class ConnectionToNotificationServer;

public:
    virtual ~Notification() override;

    const String& text() const { return m_text; }
    void set_text(const String& text)
    {
        m_text_dirty = true;
        m_text = text;
    }

    const String& title() const { return m_title; }
    void set_title(const String& title)
    {
        m_title_dirty = true;
        m_title = title;
    }

    const Gfx::Bitmap* icon() const { return m_icon; }
    void set_icon(const Gfx::Bitmap* icon)
    {
        m_icon_dirty = true;
        m_icon = icon;
    }

    void show();
    bool update();
    void close();

    bool is_showing() const { return m_shown && !m_destroyed; }

private:
    Notification();

    void connection_closed();

    String m_title;
    bool m_title_dirty;
    String m_text;
    bool m_text_dirty;
    RefPtr<Gfx::Bitmap> m_icon;
    bool m_icon_dirty;

    bool m_destroyed { false };
    bool m_shown { false };
    RefPtr<ConnectionToNotificationServer> m_connection;
};

}
