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

#include "IRCLogBuffer.h"
#include "IRCWindow.h"
#include <AK/CircularQueue.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/TCPSocket.h>

class IRCChannel;
class IRCQuery;
class IRCWindowListModel;

class IRCClient final : public Core::Object {
    C_OBJECT(IRCClient)
    friend class IRCChannel;
    friend class IRCQuery;

public:
    virtual ~IRCClient() override;

    void set_server(const String& hostname, int port = 6667);

    bool connect();

    String hostname() const { return m_hostname; }
    int port() const { return m_port; }

    String nickname() const { return m_nickname; }

    String ctcp_version_reply() const { return m_ctcp_version_reply; }
    String ctcp_userinfo_reply() const { return m_ctcp_userinfo_reply; }
    String ctcp_finger_reply() const { return m_ctcp_finger_reply; }

    bool show_join_part_messages() const { return m_show_join_part_messages; }
    bool show_nick_change_messages() const { return m_show_nick_change_messages; }

    bool notify_on_message() const { return m_notify_on_message; }
    bool notify_on_mention() const { return m_notify_on_mention; }

    void join_channel(const String&);
    void part_channel(const String&);
    void change_nick(const String&);

    static bool is_nick_prefix(char);
    static bool is_channel_prefix(char);
    String nick_without_prefix(const String& nick);

    IRCWindow* current_window() { return aid_get_active_window(); }
    const IRCWindow* current_window() const { return aid_get_active_window(); }

    Function<void()> on_disconnect;
    Function<void()> on_server_message;
    Function<void(const String&)> on_nickname_changed;
    Function<void(IRCChannel&)> on_part_from_channel;

    Function<NonnullRefPtr<IRCWindow>(void*, IRCWindow::Type, const String&)> aid_create_window;
    Function<IRCWindow*()> aid_get_active_window;
    Function<void()> aid_update_window_list;

    void register_subwindow(IRCWindow&);
    void unregister_subwindow(IRCWindow&);

    IRCWindowListModel* client_window_list_model() { return m_client_window_list_model.ptr(); }
    const IRCWindowListModel* client_window_list_model() const { return m_client_window_list_model.ptr(); }

    int window_count() const { return m_windows.size(); }
    const IRCWindow& window_at(int index) const { return *m_windows.at(index); }
    IRCWindow& window_at(int index) { return *m_windows.at(index); }

    size_t window_index(const IRCWindow& window) const
    {
        for (size_t i = 0; i < m_windows.size(); ++i) {
            if (m_windows[i] == &window)
                return i;
        }
        ASSERT_NOT_REACHED();
    }

    void did_part_from_channel(Badge<IRCChannel>, IRCChannel&);

    void handle_user_input_in_channel(const String& channel_name, const String&);
    void handle_user_input_in_query(const String& query_name, const String&);
    void handle_user_input_in_server(const String&);

    void handle_list_channels_action();
    void handle_whois_action(const String& nick);
    void handle_ctcp_user_action(const String& nick, const String& message);
    void handle_open_query_action(const String&);
    void handle_close_query_action(const String&);
    void handle_join_action(const String& channel_name);
    void handle_part_action(const String& channel_name);
    void handle_cycle_channel_action(const String& channel_name);
    void handle_change_nick_action(const String& nick);
    void handle_change_topic_action(const String& channel_name, const String&);
    void handle_invite_user_action(const String& channel_name, const String& nick);
    void handle_banlist_action(const String& channel_name);
    void handle_voice_user_action(const String& channel_name, const String& nick);
    void handle_devoice_user_action(const String& channel_name, const String& nick);
    void handle_hop_user_action(const String& channel_name, const String& nick);
    void handle_dehop_user_action(const String& channel_name, const String& nick);
    void handle_op_user_action(const String& channel_name, const String& nick);
    void handle_deop_user_action(const String& channel_name, const String& nick);
    void handle_kick_user_action(const String& channel_name, const String& nick, const String&);

    IRCQuery* query_with_name(const String&);
    IRCQuery& ensure_query(const String& name);
    IRCChannel& ensure_channel(const String& name);

    void add_server_message(const String&, Color = Color::Black);

private:
    IRCClient(String server, int port);

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
    void send_notice(const String& target, const String&);
    void send_topic(const String& channel_name, const String&);
    void send_invite(const String& channel_name, const String& nick);
    void send_banlist(const String& channel_name);
    void send_voice_user(const String& channel_name, const String& nick);
    void send_devoice_user(const String& channel_name, const String& nick);
    void send_hop_user(const String& channel_name, const String& nick);
    void send_dehop_user(const String& channel_name, const String& nick);
    void send_op_user(const String& channel_name, const String& nick);
    void send_deop_user(const String& channel_name, const String& nick);
    void send_kick(const String& channel_name, const String& nick, const String&);
    void send_list();
    void send_whois(const String&);
    void process_line(ByteBuffer&&);
    void handle_join(const Message&);
    void handle_part(const Message&);
    void handle_quit(const Message&);
    void handle_ping(const Message&);
    void handle_topic(const Message&);
    void handle_rpl_welcome(const Message&);
    void handle_rpl_topic(const Message&);
    void handle_rpl_whoisuser(const Message&);
    void handle_rpl_whoisserver(const Message&);
    void handle_rpl_whoisoperator(const Message&);
    void handle_rpl_whoisidle(const Message&);
    void handle_rpl_endofwho(const Message&);
    void handle_rpl_endofwhois(const Message&);
    void handle_rpl_endofwhowas(const Message&);
    void handle_rpl_endofmotd(const Message&);
    void handle_rpl_whoischannels(const Message&);
    void handle_rpl_topicwhotime(const Message&);
    void handle_rpl_endofnames(const Message&);
    void handle_rpl_endofbanlist(const Message&);
    void handle_rpl_namreply(const Message&);
    void handle_rpl_banlist(const Message&);
    void handle_err_nosuchnick(const Message&);
    void handle_err_unknowncommand(const Message&);
    void handle_err_nicknameinuse(const Message&);
    void handle_privmsg_or_notice(const Message&, PrivmsgOrNotice);
    void handle_nick(const Message&);
    void handle(const Message&);
    void handle_user_command(const String&);
    void handle_ctcp_request(const StringView& peer, const StringView& payload);
    void handle_ctcp_response(const StringView& peer, const StringView& payload);
    void send_ctcp_request(const StringView& peer, const StringView& payload);
    void send_ctcp_response(const StringView& peer, const StringView& payload);

    void on_socket_connected();

    String m_hostname;
    int m_port { 6667 };

    RefPtr<Core::TCPSocket> m_socket;

    String m_nickname;
    RefPtr<Core::Notifier> m_notifier;
    HashMap<String, RefPtr<IRCChannel>, CaseInsensitiveStringTraits> m_channels;
    HashMap<String, RefPtr<IRCQuery>, CaseInsensitiveStringTraits> m_queries;

    bool m_show_join_part_messages { 1 };
    bool m_show_nick_change_messages { 1 };

    bool m_notify_on_message { 1 };
    bool m_notify_on_mention { 1 };

    String m_ctcp_version_reply;
    String m_ctcp_userinfo_reply;
    String m_ctcp_finger_reply;

    Vector<IRCWindow*> m_windows;

    IRCWindow* m_server_subwindow { nullptr };

    NonnullRefPtr<IRCWindowListModel> m_client_window_list_model;
    NonnullRefPtr<IRCLogBuffer> m_log;
    NonnullRefPtr<Core::ConfigFile> m_config;
};
