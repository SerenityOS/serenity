#pragma once

#include <LibGUI/GWidget.h>

class IRCChannel;
class IRCClient;
class IRCQuery;
class IRCLogBuffer;
class GTableView;
class GTextEditor;

class IRCWindow : public GWidget {
    C_OBJECT(IRCWindow)
public:
    enum Type {
        Server,
        Channel,
        Query,
    };

    IRCWindow(IRCClient&, void* owner, Type, const String& name, GWidget* parent);
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
    GTableView* m_table_view { nullptr };
    GTextEditor* m_text_editor { nullptr };
    RefPtr<IRCLogBuffer> m_log_buffer;
    int m_unread_count { 0 };
};
