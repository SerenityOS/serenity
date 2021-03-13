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

#pragma once

#include <LibCore/Object.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class NotificationServerConnection;

class Notification : public Core::Object {
    C_OBJECT(Notification);

    friend class NotificationServerConnection;

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
    RefPtr<NotificationServerConnection> m_connection;
};

}
