#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/CircularQueue.h>
#include <AK/Function.h>
#include "IRCLogBuffer.h"
#include "IRCWindow.h"

class IRCChannel;
class IRCQuery;
class IRCWindowListModel;
class GNotifier;

class IRCClient {
    friend class IRCChannel;
    friend class IRCQuery;
public:
    IRCClient(const String& address, int port = 6667);
    ~IRCClient();

    bool connect();

    String hostname() const { return m_hostname; }
    int port() const { return m_port; }

    String nickname() const { return m_nickname; }

    void join_channel(const String&);

    bool is_nick_prefix(char) const;

    IRCWindow* current_window() { return aid_get_active_window(); }
    const IRCWindow* current_window() const { return aid_get_active_window(); }

    Function<void()> on_connect;
    Function<void()> on_disconnect;
    Function<void()> on_server_message;

    Function<IRCWindow*(void*, IRCWindow::Type, const String&)> aid_create_window;
    Function<IRCWindow*()> aid_get_active_window;
    Function<void()> aid_update_window_list;

    void register_subwindow(IRCWindow&);
    void unregister_subwindow(IRCWindow&);

    IRCWindowListModel* client_window_list_model() { return m_client_window_list_model; }
    const IRCWindowListModel* client_window_list_model() const { return m_client_window_list_model; }

    int window_count() const { return m_windows.size(); }
    const IRCWindow& window_at(int index) const { return *m_windows.at(index); }
    IRCWindow& window_at(int index) { return *m_windows.at(index); }

    void handle_user_input_in_channel(const String& channel_name, const String&);
    void handle_user_input_in_query(const String& query_name, const String&);
    void handle_user_input_in_server(const String&);

    IRCQuery& ensure_query(const String& name);
    IRCChannel& ensure_channel(const String& name);

private:
    struct Message {
        String prefix;
        String command;
        Vector<String> arguments;
    };

    void receive_from_server();
    void send(const String&);
    void send_user();
    void send_nick();
    void send_pong(const String& server);
    void send_privmsg(const String& target, const String&);
    void process_line();
    void handle_join(const Message&);
    void handle_ping(const Message&);
    void handle_namreply(const Message&);
    void handle_privmsg(const Message&);
    void handle(const Message&, const String& verbatim);

    String m_hostname;
    int m_port { 0 };
    int m_socket_fd { -1 };

    String m_nickname;
    Vector<char> m_line_buffer;
    OwnPtr<GNotifier> m_notifier;
    HashMap<String, RetainPtr<IRCChannel>> m_channels;
    HashMap<String, RetainPtr<IRCQuery>> m_queries;

    Vector<IRCWindow*> m_windows;

    IRCWindow* m_server_subwindow { nullptr };

    IRCWindowListModel* m_client_window_list_model { nullptr };

    Retained<IRCLogBuffer> m_log;
};
