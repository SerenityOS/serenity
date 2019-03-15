#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/CircularQueue.h>
#include <AK/Function.h>
#include "IRCLogBuffer.h"

class IRCChannel;
class IRCQuery;
class IRCClientWindow;
class IRCClientWindowListModel;
class GNotifier;

class IRCClient {
    friend class IRCChannel;
public:
    IRCClient(const String& address, int port = 6667);
    ~IRCClient();

    bool connect();

    String hostname() const { return m_hostname; }
    int port() const { return m_port; }

    String nickname() const { return m_nickname; }

    void join_channel(const String&);

    bool is_nick_prefix(char) const;

    Function<void()> on_connect;
    Function<void()> on_disconnect;
    Function<void(const String& channel)> on_channel_message;
    Function<void(const String& name)> on_query_message;
    Function<void(const String& channel)> on_join;
    Function<void()> on_server_message;

    void register_subwindow(IRCClientWindow&);
    void unregister_subwindow(IRCClientWindow&);

    IRCClientWindowListModel* client_window_list_model() { return m_client_window_list_model; }
    const IRCClientWindowListModel* client_window_list_model() const { return m_client_window_list_model; }

    int window_count() const { return m_windows.size(); }
    const IRCClientWindow& window_at(int index) const { return *m_windows.at(index); }
    IRCClientWindow& window_at(int index) { return *m_windows.at(index); }

    void handle_user_input_in_channel(const String& channel_name, const String&);
    void handle_user_input_in_query(const String& query_name, const String&);
    void handle_user_input_in_server(const String&);

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
    IRCQuery& ensure_query(const String& name);
    IRCChannel& ensure_channel(const String& name);

    String m_hostname;
    int m_port { 0 };
    int m_socket_fd { -1 };

    String m_nickname;
    Vector<char> m_line_buffer;
    OwnPtr<GNotifier> m_notifier;
    HashMap<String, RetainPtr<IRCChannel>> m_channels;
    HashMap<String, RetainPtr<IRCQuery>> m_queries;

    Vector<IRCClientWindow*> m_windows;

    IRCClientWindow* m_server_subwindow { nullptr };

    IRCClientWindowListModel* m_client_window_list_model { nullptr };

    Retained<IRCLogBuffer> m_log;
};
