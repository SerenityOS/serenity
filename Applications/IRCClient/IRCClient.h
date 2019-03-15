#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/CircularQueue.h>
#include <AK/Function.h>
#include "IRCLogBuffer.h"

class IRCChannel;
class IRCQuery;
class IRCSubWindow;
class GNotifier;

class IRCClient {
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
    Function<void()> on_server_message;

    void register_subwindow(IRCSubWindow&);
    void unregister_subwindow(IRCSubWindow&);

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
    void process_line();
    void handle_join(const Message&);
    void handle_ping(const Message&);
    void handle_namreply(const Message&);
    void handle_privmsg(const Message&);
    void handle(const Message&, const String& verbatim);
    IRCQuery& ensure_query(const String& name);

    String m_hostname;
    int m_port { 0 };
    int m_socket_fd { -1 };

    String m_nickname;
    Vector<char> m_line_buffer;
    OwnPtr<GNotifier> m_notifier;
    HashMap<String, RetainPtr<IRCChannel>> m_channels;
    HashMap<String, RetainPtr<IRCQuery>> m_queries;

    IRCSubWindow* m_server_subwindow { nullptr };

    Retained<IRCLogBuffer> m_log;
};
