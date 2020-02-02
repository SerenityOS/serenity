/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/GWidget.h>

namespace GUI {
class TextEditor;
}

class IRCChannel;
class IRCClient;
class IRCQuery;
class IRCLogBuffer;
class HtmlView;

class IRCWindow : public GUI::Widget {
    C_OBJECT(IRCWindow)
public:
    enum Type {
        Server,
        Channel,
        Query,
    };

    IRCWindow(IRCClient&, void* owner, Type, const String& name, GUI::Widget* parent);
    virtual ~IRCWindow() override;

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    Type type() const { return m_type; }

    void set_log_buffer(const IRCLogBuffer&);

    bool is_active() const;

    int unread_count() const;
    void clear_unread_count();

    void did_add_message();

    IRCChannel& channel() { return *(IRCChannel*)m_owner; }
    const IRCChannel& channel() const { return *(const IRCChannel*)m_owner; }

    IRCQuery& query() { return *(IRCQuery*)m_owner; }
    const IRCQuery& query() const { return *(const IRCQuery*)m_owner; }

private:
    IRCClient& m_client;
    void* m_owner { nullptr };
    Type m_type;
    String m_name;
    RefPtr<HtmlView> m_html_view;
    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<IRCLogBuffer> m_log_buffer;
    int m_unread_count { 0 };
};
