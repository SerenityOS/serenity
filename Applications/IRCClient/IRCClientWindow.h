#pragma once

#include <LibGUI/GWidget.h>

class IRCClient;
class IRCLogBuffer;
class GTableView;
class GTextEditor;

class IRCClientWindow : public GWidget {
public:
    enum Type {
        Server,
        Channel,
        Query,
    };

    explicit IRCClientWindow(IRCClient&, Type, const String& name, GWidget* parent);
    virtual ~IRCClientWindow() override;

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    Type type() const { return m_type; }

    void set_log_buffer(const IRCLogBuffer&);

private:
    IRCClient& m_client;
    Type m_type;
    String m_name;
    GTableView* m_table_view { nullptr };
    GTextEditor* m_text_editor { nullptr };
    RetainPtr<IRCLogBuffer> m_log_buffer;
};
