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

#include "IRCClient.h"
#include "IRCAppWindow.h"
#include "IRCChannel.h"
#include "IRCLogBuffer.h"
#include "IRCQuery.h"
#include "IRCWindow.h"
#include "IRCWindowListModel.h"
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/DateTime.h>
#include <LibCore/Notifier.h>
#include <pwd.h>
#include <stdio.h>
#include <strings.h>

#ifndef IRC_DEBUG
#    define IRC_DEBUG
#endif

enum IRCNumeric {
    RPL_WELCOME = 1,
    RPL_WHOISUSER = 311,
    RPL_WHOISSERVER = 312,
    RPL_WHOISOPERATOR = 313,
    RPL_ENDOFWHO = 315,
    RPL_WHOISIDLE = 317,
    RPL_ENDOFWHOIS = 318,
    RPL_WHOISCHANNELS = 319,
    RPL_TOPIC = 332,
    RPL_TOPICWHOTIME = 333,
    RPL_NAMREPLY = 353,
    RPL_ENDOFNAMES = 366,
    RPL_BANLIST = 367,
    RPL_ENDOFBANLIST = 368,
    RPL_ENDOFWHOWAS = 369,
    RPL_ENDOFMOTD = 376,
    ERR_NOSUCHNICK = 401,
    ERR_UNKNOWNCOMMAND = 421,
    ERR_NICKNAMEINUSE = 433,
};

IRCClient::IRCClient(String server, int port)
    : m_nickname("seren1ty")
    , m_client_window_list_model(IRCWindowListModel::create(*this))
    , m_log(IRCLogBuffer::create())
    , m_config(Core::ConfigFile::get_for_app("IRCClient"))
{
    struct passwd* user_pw = getpwuid(getuid());
    m_socket = Core::TCPSocket::construct(this);
    m_nickname = m_config->read_entry("User", "Nickname", String::formatted("{}_seren1ty", user_pw->pw_name));

    if (server.is_empty()) {
        m_hostname = m_config->read_entry("Connection", "Server", "");
        m_port = m_config->read_num_entry("Connection", "Port", 6667);
    } else {
        m_hostname = server;
        m_port = port ? port : 6667;
    }

    m_show_join_part_messages = m_config->read_bool_entry("Messaging", "ShowJoinPartMessages", 1);
    m_show_nick_change_messages = m_config->read_bool_entry("Messaging", "ShowNickChangeMessages", 1);

    m_notify_on_message = m_config->read_bool_entry("Notifications", "NotifyOnMessage", 1);
    m_notify_on_mention = m_config->read_bool_entry("Notifications", "NotifyOnMention", 1);

    m_ctcp_version_reply = m_config->read_entry("CTCP", "VersionReply", "IRC Client [x86] / Serenity OS");
    m_ctcp_userinfo_reply = m_config->read_entry("CTCP", "UserInfoReply", user_pw->pw_name);
    m_ctcp_finger_reply = m_config->read_entry("CTCP", "FingerReply", user_pw->pw_name);
}

IRCClient::~IRCClient()
{
}

void IRCClient::set_server(const String& hostname, int port)
{
    m_hostname = hostname;
    m_port = port;
    m_config->write_entry("Connection", "Server", hostname);
    m_config->write_num_entry("Connection", "Port", port);
    m_config->sync();
}

void IRCClient::on_socket_connected()
{
    m_notifier = Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] { receive_from_server(); };

    send_user();
    send_nick();
}

bool IRCClient::connect()
{
    if (m_socket->is_connected())
        ASSERT_NOT_REACHED();

    m_socket->on_connected = [this] { on_socket_connected(); };
    bool success = m_socket->connect(m_hostname, m_port);
    if (!success)
        return false;
    return true;
}

void IRCClient::receive_from_server()
{
    while (m_socket->can_read_line()) {
        auto line = m_socket->read_line(PAGE_SIZE);
        if (line.is_null()) {
            if (!m_socket->is_connected()) {
                outln("IRCClient: Connection closed!");
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }
        process_line(move(line));
    }
}

void IRCClient::process_line(ByteBuffer&& line)
{
    Message msg;
    Vector<char, 32> prefix;
    Vector<char, 32> command;
    Vector<char, 256> current_parameter;
    enum {
        Start,
        InPrefix,
        InCommand,
        InStartOfParameter,
        InParameter,
        InTrailingParameter,
    } state
        = Start;

    for (size_t i = 0; i < line.size(); ++i) {
        char ch = line[i];
        if (ch == '\r')
            continue;
        if (ch == '\n')
            break;
        switch (state) {
        case Start:
            if (ch == ':') {
                state = InPrefix;
                continue;
            }
            state = InCommand;
            [[fallthrough]];
        case InCommand:
            if (ch == ' ') {
                state = InStartOfParameter;
                continue;
            }
            command.append(ch);
            continue;
        case InPrefix:
            if (ch == ' ') {
                state = InCommand;
                continue;
            }
            prefix.append(ch);
            continue;
        case InStartOfParameter:
            if (ch == ':') {
                state = InTrailingParameter;
                continue;
            }
            state = InParameter;
            [[fallthrough]];
        case InParameter:
            if (ch == ' ') {
                if (!current_parameter.is_empty())
                    msg.arguments.append(String(current_parameter.data(), current_parameter.size()));
                current_parameter.clear_with_capacity();
                state = InStartOfParameter;
                continue;
            }
            current_parameter.append(ch);
            continue;
        case InTrailingParameter:
            current_parameter.append(ch);
            continue;
        }
    }
    if (!current_parameter.is_empty())
        msg.arguments.append(String::copy(current_parameter));
    msg.prefix = String::copy(prefix);
    msg.command = String::copy(command);
    handle(msg);
}

void IRCClient::send(const String& text)
{
    if (!m_socket->send(text.bytes())) {
        perror("send");
        exit(1);
    }
}

void IRCClient::send_user()
{
    send(String::formatted("USER {} 0 * :{}\r\n", m_nickname, m_nickname));
}

void IRCClient::send_nick()
{
    send(String::formatted("NICK {}\r\n", m_nickname));
}

void IRCClient::send_pong(const String& server)
{
    send(String::formatted("PONG {}\r\n", server));
    sleep(1);
}

void IRCClient::join_channel(const String& channel_name)
{
    send(String::formatted("JOIN {}\r\n", channel_name));
}

void IRCClient::part_channel(const String& channel_name)
{
    send(String::formatted("PART {}\r\n", channel_name));
}

void IRCClient::send_whois(const String& nick)
{
    send(String::formatted("WHOIS {}\r\n", nick));
}

void IRCClient::handle(const Message& msg)
{
#ifdef IRC_DEBUG
    outln("IRCClient::execute: prefix='{}', command='{}', arguments={}",
        msg.prefix,
        msg.command,
        msg.arguments.size());

    size_t index = 0;
    for (auto& arg : msg.arguments)
        outln("    [{}]: {}", index++, arg);
#endif

    auto numeric = msg.command.to_uint();

    if (numeric.has_value()) {
        switch (numeric.value()) {
        case RPL_WELCOME:
            return handle_rpl_welcome(msg);
        case RPL_WHOISCHANNELS:
            return handle_rpl_whoischannels(msg);
        case RPL_ENDOFWHO:
            return handle_rpl_endofwho(msg);
        case RPL_ENDOFWHOIS:
            return handle_rpl_endofwhois(msg);
        case RPL_ENDOFWHOWAS:
            return handle_rpl_endofwhowas(msg);
        case RPL_ENDOFMOTD:
            return handle_rpl_endofmotd(msg);
        case RPL_WHOISOPERATOR:
            return handle_rpl_whoisoperator(msg);
        case RPL_WHOISSERVER:
            return handle_rpl_whoisserver(msg);
        case RPL_WHOISUSER:
            return handle_rpl_whoisuser(msg);
        case RPL_WHOISIDLE:
            return handle_rpl_whoisidle(msg);
        case RPL_TOPICWHOTIME:
            return handle_rpl_topicwhotime(msg);
        case RPL_TOPIC:
            return handle_rpl_topic(msg);
        case RPL_NAMREPLY:
            return handle_rpl_namreply(msg);
        case RPL_ENDOFNAMES:
            return handle_rpl_endofnames(msg);
        case RPL_BANLIST:
            return handle_rpl_banlist(msg);
        case RPL_ENDOFBANLIST:
            return handle_rpl_endofbanlist(msg);
        case ERR_NOSUCHNICK:
            return handle_err_nosuchnick(msg);
        case ERR_UNKNOWNCOMMAND:
            return handle_err_unknowncommand(msg);
        case ERR_NICKNAMEINUSE:
            return handle_err_nicknameinuse(msg);
        }
    }

    if (msg.command == "PING")
        return handle_ping(msg);

    if (msg.command == "JOIN")
        return handle_join(msg);

    if (msg.command == "PART")
        return handle_part(msg);

    if (msg.command == "QUIT")
        return handle_quit(msg);

    if (msg.command == "TOPIC")
        return handle_topic(msg);

    if (msg.command == "PRIVMSG")
        return handle_privmsg_or_notice(msg, PrivmsgOrNotice::Privmsg);

    if (msg.command == "NOTICE")
        return handle_privmsg_or_notice(msg, PrivmsgOrNotice::Notice);

    if (msg.command == "NICK")
        return handle_nick(msg);

    if (msg.arguments.size() >= 2)
        add_server_message(String::formatted("[{}] {}", msg.command, msg.arguments[1]));
}

void IRCClient::add_server_message(const String& text, Color color)
{
    m_log->add_message(0, "", text, color);
    m_server_subwindow->did_add_message();
}

void IRCClient::send_topic(const String& channel_name, const String& text)
{
    send(String::formatted("TOPIC {} :{}\r\n", channel_name, text));
}

void IRCClient::send_invite(const String& channel_name, const String& nick)
{
    send(String::formatted("INVITE {} {}\r\n", nick, channel_name));
}

void IRCClient::send_banlist(const String& channel_name)
{
    send(String::formatted("MODE {} +b\r\n", channel_name));
}

void IRCClient::send_voice_user(const String& channel_name, const String& nick)
{
    send(String::formatted("MODE {} +v {}\r\n", channel_name, nick));
}

void IRCClient::send_devoice_user(const String& channel_name, const String& nick)
{
    send(String::formatted("MODE {} -v {}\r\n", channel_name, nick));
}

void IRCClient::send_hop_user(const String& channel_name, const String& nick)
{
    send(String::formatted("MODE {} +h {}\r\n", channel_name, nick));
}

void IRCClient::send_dehop_user(const String& channel_name, const String& nick)
{
    send(String::formatted("MODE {} -h {}\r\n", channel_name, nick));
}

void IRCClient::send_op_user(const String& channel_name, const String& nick)
{
    send(String::formatted("MODE {} +o {}\r\n", channel_name, nick));
}

void IRCClient::send_deop_user(const String& channel_name, const String& nick)
{
    send(String::formatted("MODE {} -o {}\r\n", channel_name, nick));
}

void IRCClient::send_kick(const String& channel_name, const String& nick, const String& comment)
{
    send(String::formatted("KICK {} {} :{}\r\n", channel_name, nick, comment));
}

void IRCClient::send_list()
{
    send("LIST\r\n");
}

void IRCClient::send_privmsg(const String& target, const String& text)
{
    send(String::formatted("PRIVMSG {} :{}\r\n", target, text));
}

void IRCClient::send_notice(const String& target, const String& text)
{
    send(String::formatted("NOTICE {} :{}\r\n", target, text));
}

void IRCClient::handle_user_input_in_channel(const String& channel_name, const String& input)
{
    if (input.is_empty())
        return;
    if (input[0] == '/')
        return handle_user_command(input);
    ensure_channel(channel_name).say(input);
}

void IRCClient::handle_user_input_in_query(const String& query_name, const String& input)
{
    if (input.is_empty())
        return;
    if (input[0] == '/')
        return handle_user_command(input);
    ensure_query(query_name).say(input);
}

void IRCClient::handle_user_input_in_server(const String& input)
{
    if (input.is_empty())
        return;
    if (input[0] == '/')
        return handle_user_command(input);
}

String IRCClient::nick_without_prefix(const String& nick)
{
    assert(!nick.is_empty());
    if (IRCClient::is_nick_prefix(nick[0]))
        return nick.substring(1, nick.length() - 1);
    return nick;
}

bool IRCClient::is_nick_prefix(char ch)
{
    switch (ch) {
    case '@':
    case '+':
    case '~':
    case '&':
    case '%':
        return true;
    }
    return false;
}

bool IRCClient::is_channel_prefix(char ch)
{
    switch (ch) {
    case '&':
    case '#':
    case '+':
    case '!':
        return true;
    }
    return false;
}

static bool has_ctcp_payload(const StringView& string)
{
    return string.length() >= 2 && string[0] == 0x01 && string[string.length() - 1] == 0x01;
}

void IRCClient::handle_privmsg_or_notice(const Message& msg, PrivmsgOrNotice type)
{
    if (msg.arguments.size() < 2)
        return;
    if (msg.prefix.is_empty())
        return;
    auto parts = msg.prefix.split('!');
    auto sender_nick = parts[0];
    auto target = msg.arguments[0];

    bool is_ctcp = has_ctcp_payload(msg.arguments[1]);

#ifdef IRC_DEBUG
    outln("handle_privmsg_or_notice: type='{}'{}, sender_nick='{}', target='{}'",
        type == PrivmsgOrNotice::Privmsg ? "privmsg" : "notice",
        is_ctcp ? " (ctcp)" : "",
        sender_nick,
        target);
#endif

    if (sender_nick.is_empty())
        return;

    char sender_prefix = 0;
    if (is_nick_prefix(sender_nick[0])) {
        sender_prefix = sender_nick[0];
        sender_nick = sender_nick.substring(1, sender_nick.length() - 1);
    }

    String message_text = msg.arguments[1];
    auto message_color = Color::Black;

    bool insert_as_raw_message = false;

    if (is_ctcp) {
        auto ctcp_payload = msg.arguments[1].substring_view(1, msg.arguments[1].length() - 2);
        if (type == PrivmsgOrNotice::Privmsg)
            handle_ctcp_request(sender_nick, ctcp_payload);
        else
            handle_ctcp_response(sender_nick, ctcp_payload);

        if (ctcp_payload.starts_with("ACTION")) {
            insert_as_raw_message = true;
            message_text = String::formatted("* {}{}", sender_nick, ctcp_payload.substring_view(6, ctcp_payload.length() - 6));
            message_color = Color::Magenta;
        } else {
            message_text = String::formatted("(CTCP) {}", ctcp_payload);
            message_color = Color::Blue;
        }
    }

    {
        auto it = m_channels.find(target);
        if (it != m_channels.end()) {
            if (insert_as_raw_message)
                (*it).value->add_message(message_text, message_color);
            else
                (*it).value->add_message(sender_prefix, sender_nick, message_text, message_color);
            return;
        }
    }

    // For NOTICE or CTCP messages, only put them in query if one already exists.
    // Otherwise, put them in the server window. This seems to match other clients.
    IRCQuery* query = nullptr;
    if (is_ctcp || type == PrivmsgOrNotice::Notice) {
        query = query_with_name(sender_nick);
    } else {
        query = &ensure_query(sender_nick);
    }
    if (query) {
        if (insert_as_raw_message)
            query->add_message(message_text, message_color);
        else
            query->add_message(sender_prefix, sender_nick, message_text, message_color);
    } else {
        add_server_message(String::formatted("<{}> {}", sender_nick, message_text), message_color);
    }
}

IRCQuery* IRCClient::query_with_name(const String& name)
{
    return const_cast<IRCQuery*>(m_queries.get(name).value_or(nullptr));
}

IRCQuery& IRCClient::ensure_query(const String& name)
{
    auto it = m_queries.find(name);
    if (it != m_queries.end())
        return *(*it).value;
    auto query = IRCQuery::create(*this, name);
    auto& query_reference = *query;
    m_queries.set(name, query);
    return query_reference;
}

IRCChannel& IRCClient::ensure_channel(const String& name)
{
    auto it = m_channels.find(name);
    if (it != m_channels.end())
        return *(*it).value;
    auto channel = IRCChannel::create(*this, name);
    auto& channel_reference = *channel;
    m_channels.set(name, channel);
    return channel_reference;
}

void IRCClient::handle_ping(const Message& msg)
{
    if (msg.arguments.size() < 1)
        return;
    m_log->add_message(0, "", "Ping? Pong!");
    send_pong(msg.arguments[0]);
}

void IRCClient::handle_join(const Message& msg)
{
    if (msg.arguments.size() != 1)
        return;
    auto prefix_parts = msg.prefix.split('!');
    if (prefix_parts.size() < 1)
        return;
    auto nick = prefix_parts[0];
    auto& channel_name = msg.arguments[0];
    ensure_channel(channel_name).handle_join(nick, msg.prefix);
}

void IRCClient::handle_part(const Message& msg)
{
    if (msg.arguments.size() < 1)
        return;
    auto prefix_parts = msg.prefix.split('!');
    if (prefix_parts.size() < 1)
        return;
    auto nick = prefix_parts[0];
    auto& channel_name = msg.arguments[0];
    ensure_channel(channel_name).handle_part(nick, msg.prefix);
}

void IRCClient::handle_quit(const Message& msg)
{
    if (msg.arguments.size() < 1)
        return;
    auto prefix_parts = msg.prefix.split('!');
    if (prefix_parts.size() < 1)
        return;
    auto nick = prefix_parts[0];
    auto& message = msg.arguments[0];
    for (auto& it : m_channels) {
        it.value->handle_quit(nick, msg.prefix, message);
    }
}

void IRCClient::handle_nick(const Message& msg)
{
    auto prefix_parts = msg.prefix.split('!');
    if (prefix_parts.size() < 1)
        return;
    auto old_nick = prefix_parts[0];
    if (msg.arguments.size() != 1)
        return;
    auto& new_nick = msg.arguments[0];
    if (old_nick == m_nickname)
        m_nickname = new_nick;
    if (m_show_nick_change_messages)
        add_server_message(String::formatted("~ {} changed nickname to {}", old_nick, new_nick));
    if (on_nickname_changed)
        on_nickname_changed(new_nick);
    for (auto& it : m_channels) {
        it.value->notify_nick_changed(old_nick, new_nick);
    }
}

void IRCClient::handle_topic(const Message& msg)
{
    if (msg.arguments.size() != 2)
        return;
    auto prefix_parts = msg.prefix.split('!');
    if (prefix_parts.size() < 1)
        return;
    auto nick = prefix_parts[0];
    auto& channel_name = msg.arguments[0];
    ensure_channel(channel_name).handle_topic(nick, msg.arguments[1]);
}

void IRCClient::handle_rpl_welcome(const Message& msg)
{
    if (msg.arguments.size() < 2)
        return;
    auto& welcome_message = msg.arguments[1];
    add_server_message(welcome_message);

    auto channel_str = m_config->read_entry("Connection", "AutoJoinChannels", "");
    if (channel_str.is_empty())
        return;
    dbgln("IRCClient: Channels to autojoin: {}", channel_str);
    auto channels = channel_str.split(',');
    for (auto& channel : channels) {
        join_channel(channel);
        dbgln("IRCClient: Auto joining channel: {}", channel);
    }
}

void IRCClient::handle_rpl_topic(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& channel_name = msg.arguments[1];
    auto& topic = msg.arguments[2];
    ensure_channel(channel_name).handle_topic({}, topic);
}

void IRCClient::handle_rpl_namreply(const Message& msg)
{
    if (msg.arguments.size() < 4)
        return;
    auto& channel_name = msg.arguments[2];
    auto& channel = ensure_channel(channel_name);
    auto members = msg.arguments[3].split(' ');

    quick_sort(members, [](auto& a, auto& b) {
        return strcasecmp(a.characters(), b.characters()) < 0;
    });

    for (auto& member : members) {
        if (member.is_empty())
            continue;
        char prefix = 0;
        if (is_nick_prefix(member[0]))
            prefix = member[0];
        channel.add_member(member, prefix);
    }
}

void IRCClient::handle_rpl_endofnames(const Message&)
{
    add_server_message("// End of NAMES");
}

void IRCClient::handle_rpl_banlist(const Message& msg)
{
    if (msg.arguments.size() < 5)
        return;
    auto& channel = msg.arguments[1];
    auto& mask = msg.arguments[2];
    auto& user = msg.arguments[3];
    auto& datestamp = msg.arguments[4];
    add_server_message(String::formatted("* {}: {} on {} by {}", channel, mask, datestamp, user));
}

void IRCClient::handle_rpl_endofbanlist(const Message&)
{
    add_server_message("// End of BANLIST");
}

void IRCClient::handle_rpl_endofwho(const Message&)
{
    add_server_message("// End of WHO");
}

void IRCClient::handle_rpl_endofwhois(const Message&)
{
    add_server_message("// End of WHOIS");
}

void IRCClient::handle_rpl_endofwhowas(const Message&)
{
    add_server_message("// End of WHOWAS");
}

void IRCClient::handle_rpl_endofmotd(const Message&)
{
    add_server_message("// End of MOTD");
}

void IRCClient::handle_rpl_whoisoperator(const Message& msg)
{
    if (msg.arguments.size() < 2)
        return;
    auto& nick = msg.arguments[1];
    add_server_message(String::formatted("* {} is an IRC operator", nick));
}

void IRCClient::handle_rpl_whoisserver(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& server = msg.arguments[2];
    add_server_message(String::formatted("* {} is using server {}", nick, server));
}

void IRCClient::handle_rpl_whoisuser(const Message& msg)
{
    if (msg.arguments.size() < 6)
        return;
    auto& nick = msg.arguments[1];
    auto& username = msg.arguments[2];
    auto& host = msg.arguments[3];
    auto& asterisk = msg.arguments[4];
    auto& realname = msg.arguments[5];
    (void)asterisk;
    add_server_message(String::formatted("* {} is {}@{}, real name: {}", nick, username, host, realname));
}

void IRCClient::handle_rpl_whoisidle(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& secs = msg.arguments[2];
    add_server_message(String::formatted("* {} is {} seconds idle", nick, secs));
}

void IRCClient::handle_rpl_whoischannels(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& channel_list = msg.arguments[2];
    add_server_message(String::formatted("* {} is in channels {}", nick, channel_list));
}

void IRCClient::handle_rpl_topicwhotime(const Message& msg)
{
    if (msg.arguments.size() < 4)
        return;
    auto& channel_name = msg.arguments[1];
    auto& nick = msg.arguments[2];
    auto setat = msg.arguments[3];
    auto setat_time = setat.to_uint();
    if (setat_time.has_value())
        setat = Core::DateTime::from_timestamp(setat_time.value()).to_string();
    ensure_channel(channel_name).add_message(String::formatted("*** (set by {} at {})", nick, setat), Color::Blue);
}

void IRCClient::handle_err_nosuchnick(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& message = msg.arguments[2];
    add_server_message(String::formatted("* {} :{}", nick, message));
}

void IRCClient::handle_err_unknowncommand(const Message& msg)
{
    if (msg.arguments.size() < 2)
        return;
    auto& cmd = msg.arguments[1];
    add_server_message(String::formatted("* Unknown command: {}", cmd));
}

void IRCClient::handle_err_nicknameinuse(const Message& msg)
{
    if (msg.arguments.size() < 2)
        return;
    auto& nick = msg.arguments[1];
    add_server_message(String::formatted("* {} :Nickname in use", nick));
}

void IRCClient::register_subwindow(IRCWindow& subwindow)
{
    if (subwindow.type() == IRCWindow::Server) {
        m_server_subwindow = &subwindow;
        subwindow.set_log_buffer(*m_log);
    }
    m_windows.append(&subwindow);
    m_client_window_list_model->update();
}

void IRCClient::unregister_subwindow(IRCWindow& subwindow)
{
    if (subwindow.type() == IRCWindow::Server) {
        m_server_subwindow = &subwindow;
    }
    for (size_t i = 0; i < m_windows.size(); ++i) {
        if (m_windows.at(i) == &subwindow) {
            m_windows.remove(i);
            break;
        }
    }
    m_client_window_list_model->update();
}

void IRCClient::handle_user_command(const String& input)
{
    auto parts = input.split_view(' ');
    if (parts.is_empty())
        return;
    auto command = String(parts[0]).to_uppercase();
    if (command == "/RAW") {
        if (parts.size() <= 1)
            return;
        int command_length = command.length() + 1;
        StringView raw_message = input.view().substring_view(command_length, input.view().length() - command_length);
        send(String::formatted("{}\r\n", raw_message));
        return;
    }
    if (command == "/NICK") {
        if (parts.size() >= 2)
            change_nick(parts[1]);
        return;
    }
    if (command == "/JOIN") {
        if (parts.size() >= 2)
            join_channel(parts[1]);
        return;
    }
    if (command == "/PART") {
        if (parts.size() >= 2) {
            auto channel = parts[1];
            part_channel(channel);
        } else {
            auto* window = current_window();
            if (!window || window->type() != IRCWindow::Type::Channel)
                return;
            auto channel = window->channel().name();
            join_channel(channel);
        }
        return;
    }
    if (command == "/CYCLE") {
        if (parts.size() >= 2) {
            auto channel = parts[1];
            part_channel(channel);
            join_channel(channel);
        } else {
            auto* window = current_window();
            if (!window || window->type() != IRCWindow::Type::Channel)
                return;
            auto channel = window->channel().name();
            part_channel(channel);
            join_channel(channel);
        }
        return;
    }
    if (command == "/BANLIST") {
        if (parts.size() >= 2) {
            auto channel = parts[1];
            send_banlist(channel);
        } else {
            auto* window = current_window();
            if (!window || window->type() != IRCWindow::Type::Channel)
                return;
            auto channel = window->channel().name();
            send_banlist(channel);
        }
        return;
    }
    if (command == "/ME") {
        if (parts.size() < 2)
            return;

        auto* window = current_window();
        if (!window)
            return;

        auto emote = input.view().substring_view_starting_after_substring(parts[0]);
        auto action_string = String::formatted("ACTION{}", emote);
        String peer;
        if (window->type() == IRCWindow::Type::Channel) {
            peer = window->channel().name();
            window->channel().add_message(String::formatted("* {}{}", m_nickname, emote), Gfx::Color::Magenta);
        } else if (window->type() == IRCWindow::Type::Query) {
            peer = window->query().name();
            window->query().add_message(String::formatted("* {}{}", m_nickname, emote), Gfx::Color::Magenta);
        } else {
            return;
        }

        send_ctcp_request(peer, action_string);
        return;
    }
    if (command == "/TOPIC") {
        if (parts.size() < 2)
            return;
        if (parts[1].is_empty())
            return;

        if (is_channel_prefix(parts[1][0])) {
            if (parts.size() < 3)
                return;
            auto channel = parts[1];
            auto topic = input.view().substring_view_starting_after_substring(channel);
            send_topic(channel, topic);
        } else {
            auto* window = current_window();
            if (!window || window->type() != IRCWindow::Type::Channel)
                return;
            auto channel = window->channel().name();
            auto topic = input.view().substring_view_starting_after_substring(parts[0]);
            send_topic(channel, topic);
        }
        return;
    }
    if (command == "/KICK") {
        if (parts.size() < 2)
            return;
        if (parts[1].is_empty())
            return;

        if (is_channel_prefix(parts[1][0])) {
            if (parts.size() < 3)
                return;
            auto channel = parts[1];
            auto nick = parts[2];
            auto reason = input.view().substring_view_starting_after_substring(nick);
            send_kick(channel, nick, reason);
        } else {
            auto* window = current_window();
            if (!window || window->type() != IRCWindow::Type::Channel)
                return;
            auto channel = window->channel().name();
            auto nick = parts[1];
            auto reason = input.view().substring_view_starting_after_substring(nick);
            send_kick(channel, nick, reason);
        }
        return;
    }
    if (command == "/LIST") {
        send_list();
        return;
    }
    if (command == "/QUERY") {
        if (parts.size() >= 2) {
            auto& query = ensure_query(parts[1]);
            IRCAppWindow::the().set_active_window(query.window());
        }
        return;
    }
    if (command == "/MSG") {
        if (parts.size() < 3)
            return;
        auto nick = parts[1];
        auto& query = ensure_query(nick);
        IRCAppWindow::the().set_active_window(query.window());
        query.say(input.view().substring_view_starting_after_substring(nick));
        return;
    }
    if (command == "/WHOIS") {
        if (parts.size() >= 2)
            send_whois(parts[1]);
        return;
    }
}

void IRCClient::change_nick(const String& nick)
{
    send(String::formatted("NICK {}\r\n", nick));
}

void IRCClient::handle_list_channels_action()
{
    send_list();
}

void IRCClient::handle_whois_action(const String& nick)
{
    send_whois(nick);
}

void IRCClient::handle_ctcp_user_action(const String& nick, const String& message)
{
    send_ctcp_request(nick, message);
}

void IRCClient::handle_open_query_action(const String& nick)
{
    ensure_query(nick);
}

void IRCClient::handle_change_nick_action(const String& nick)
{
    change_nick(nick);
}

void IRCClient::handle_change_topic_action(const String& channel, const String& topic)
{
    send_topic(channel, topic);
}

void IRCClient::handle_invite_user_action(const String& channel, const String& nick)
{
    send_invite(channel, nick);
}

void IRCClient::handle_banlist_action(const String& channel)
{
    send_banlist(channel);
}

void IRCClient::handle_voice_user_action(const String& channel, const String& nick)
{
    send_voice_user(channel, nick);
}

void IRCClient::handle_devoice_user_action(const String& channel, const String& nick)
{
    send_devoice_user(channel, nick);
}

void IRCClient::handle_hop_user_action(const String& channel, const String& nick)
{
    send_hop_user(channel, nick);
}

void IRCClient::handle_dehop_user_action(const String& channel, const String& nick)
{
    send_dehop_user(channel, nick);
}

void IRCClient::handle_op_user_action(const String& channel, const String& nick)
{
    send_op_user(channel, nick);
}

void IRCClient::handle_deop_user_action(const String& channel, const String& nick)
{
    send_deop_user(channel, nick);
}

void IRCClient::handle_kick_user_action(const String& channel, const String& nick, const String& message)
{
    send_kick(channel, nick, message);
}

void IRCClient::handle_close_query_action(const String& nick)
{
    m_queries.remove(nick);
    m_client_window_list_model->update();
}

void IRCClient::handle_join_action(const String& channel)
{
    join_channel(channel);
}

void IRCClient::handle_part_action(const String& channel)
{
    part_channel(channel);
}

void IRCClient::handle_cycle_channel_action(const String& channel)
{
    part_channel(channel);
    join_channel(channel);
}

void IRCClient::did_part_from_channel(Badge<IRCChannel>, IRCChannel& channel)
{
    if (on_part_from_channel)
        on_part_from_channel(channel);
}

void IRCClient::send_ctcp_response(const StringView& peer, const StringView& payload)
{
    StringBuilder builder;
    builder.append(0x01);
    builder.append(payload);
    builder.append(0x01);
    auto message = builder.to_string();
    send_notice(peer, message);
}

void IRCClient::send_ctcp_request(const StringView& peer, const StringView& payload)
{
    StringBuilder builder;
    builder.append(0x01);
    builder.append(payload);
    builder.append(0x01);
    auto message = builder.to_string();
    send_privmsg(peer, message);
}

void IRCClient::handle_ctcp_request(const StringView& peer, const StringView& payload)
{
    dbgln("handle_ctcp_request: {}", payload);

    if (payload == "VERSION") {
        auto version = ctcp_version_reply();
        if (version.is_empty())
            return;
        send_ctcp_response(peer, String::formatted("VERSION {}", version));
        return;
    }

    if (payload == "USERINFO") {
        auto userinfo = ctcp_userinfo_reply();
        if (userinfo.is_empty())
            return;
        send_ctcp_response(peer, String::formatted("USERINFO {}", userinfo));
        return;
    }

    if (payload == "FINGER") {
        auto finger = ctcp_finger_reply();
        if (finger.is_empty())
            return;
        send_ctcp_response(peer, String::formatted("FINGER {}", finger));
        return;
    }

    if (payload.starts_with("PING")) {
        send_ctcp_response(peer, payload);
        return;
    }
}

void IRCClient::handle_ctcp_response(const StringView& peer, const StringView& payload)
{
    dbgln("handle_ctcp_response({}): {}", peer, payload);
}
