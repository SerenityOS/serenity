#pragma once

#include "IRCLogBuffer.h"
#include "IRCWindow.h"
#include <AK/AKString.h>
#include <AK/CircularQueue.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CTCPSocket.h>

class IRCChannel;
class IRCQuery;
class IRCWindowListModel;
class CNotifier;

class IRCClient final : public CObject {
    C_OBJECT(IRCClient)
    friend class IRCChannel;
    friend class IRCQuery;
public:
    IRCClient();
    virtual ~IRCClient() override;

    void set_server(const String& hostname, int port = 6667);

    bool connect();

    String hostname() const { return m_hostname; }
    int port() const { return m_port; }

    String nickname() const { return m_nickname; }

    void join_channel(const String&);
    void part_channel(const String&);
    void change_nick(const String&);

    bool is_nick_prefix(char) const;

    IRCWindow* current_window() { return aid_get_active_window(); }
    const IRCWindow* current_window() const { return aid_get_active_window(); }

    Function<void()> on_connect;
    Function<void()> on_disconnect;
    Function<void()> on_server_message;
    Function<void(const String&)> on_nickname_changed;
    Function<void(IRCChannel&)> on_part_from_channel;

    Function<IRCWindow*(void*, IRCWindow::Type, const String&)> aid_create_window;
    Function<IRCWindow*()> aid_get_active_window;
    Function<void()> aid_update_window_list;

    void register_subwindow(IRCWindow&);
    void unregister_subwindow(IRCWindow&);

    IRCWindowListModel* client_window_list_model() { return m_client_window_list_model.ptr(); }
    const IRCWindowListModel* client_window_list_model() const { return m_client_window_list_model.ptr(); }

    int window_count() const { return m_windows.size(); }
    const IRCWindow& window_at(int index) const { return *m_windows.at(index); }
    IRCWindow& window_at(int index) { return *m_windows.at(index); }

    int window_index(const IRCWindow& window) const
    {
        for (int i = 0; i < m_windows.size(); ++i) {
            if (m_windows[i] == &window)
                return i;
        }
        return -1;
    }

    void did_part_from_channel(Badge<IRCChannel>, IRCChannel&);

    void handle_user_input_in_channel(const String& channel_name, const String&);
    void handle_user_input_in_query(const String& query_name, const String&);
    void handle_user_input_in_server(const String&);

    void handle_whois_action(const String&);
    void handle_open_query_action(const String&);
    void handle_close_query_action(const String&);
    void handle_join_action(const String&);
    void handle_part_action(const String&);
    void handle_change_nick_action(const String&);

    IRCQuery& ensure_query(const String& name);
    IRCChannel& ensure_channel(const String& name);

    void add_server_message(const String&);

private:
    struct Message {
        String prefix;
        String command;
        Vector<String> arguments;
    };

    enum class PrivmsgOrNotice {
        Privmsg,
        Notice,
    };

    void receive_from_server();
    void send(const String&);
    void send_user();
    void send_nick();
    void send_pong(const String& server);
    void send_privmsg(const String& target, const String&);
    void send_whois(const String&);
    void process_line(ByteBuffer&&);
    void handle_join(const Message&);
    void handle_part(const Message&);
    void handle_ping(const Message&);
    void handle_topic(const Message&);
    void handle_rpl_topic(const Message&);
    void handle_rpl_whoisuser(const Message&);
    void handle_rpl_whoisserver(const Message&);
    void handle_rpl_whoisoperator(const Message&);
    void handle_rpl_whoisidle(const Message&);
    void handle_rpl_endofwhois(const Message&);
    void handle_rpl_whoischannels(const Message&);
    void handle_rpl_topicwhotime(const Message&);
    void handle_rpl_endofnames(const Message&);
    void handle_rpl_namreply(const Message&);
    void handle_privmsg_or_notice(const Message&, PrivmsgOrNotice);
    void handle_nick(const Message&);
    void handle(const Message&);
    void handle_user_command(const String&);

    void on_socket_connected();

    String m_hostname;
    int m_port { 6667 };

    CTCPSocket* m_socket { nullptr };

    String m_nickname;
    OwnPtr<CNotifier> m_notifier;
    HashMap<String, RefPtr<IRCChannel>, CaseInsensitiveStringTraits> m_channels;
    HashMap<String, RefPtr<IRCQuery>, CaseInsensitiveStringTraits> m_queries;

    Vector<IRCWindow*> m_windows;

    IRCWindow* m_server_subwindow { nullptr };

    NonnullRefPtr<IRCWindowListModel> m_client_window_list_model;
    NonnullRefPtr<IRCLogBuffer> m_log;
    NonnullRefPtr<CConfigFile> m_config;
};
