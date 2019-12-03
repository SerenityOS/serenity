#include "IRCClient.h"
#include "IRCAppWindow.h"
#include "IRCChannel.h"
#include "IRCLogBuffer.h"
#include "IRCQuery.h"
#include "IRCWindow.h"
#include "IRCWindowListModel.h"
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/CNotifier.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

#define IRC_DEBUG

enum IRCNumeric {
    RPL_WHOISUSER = 311,
    RPL_WHOISSERVER = 312,
    RPL_WHOISOPERATOR = 313,
    RPL_WHOISIDLE = 317,
    RPL_ENDOFWHOIS = 318,
    RPL_WHOISCHANNELS = 319,
    RPL_TOPIC = 332,
    RPL_TOPICWHOTIME = 333,
    RPL_NAMREPLY = 353,
    RPL_ENDOFNAMES = 366,
};

IRCClient::IRCClient()
    : m_nickname("seren1ty")
    , m_client_window_list_model(IRCWindowListModel::create(*this))
    , m_log(IRCLogBuffer::create())
    , m_config(CConfigFile::get_for_app("IRCClient"))
{
    m_socket = CTCPSocket::construct(this);
    m_nickname = m_config->read_entry("User", "Nickname", "seren1ty");
    m_hostname = m_config->read_entry("Connection", "Server", "");
    m_port = m_config->read_num_entry("Connection", "Port", 6667);
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
    m_notifier = CNotifier::construct(m_socket->fd(), CNotifier::Read);
    m_notifier->on_ready_to_read = [this] { receive_from_server(); };

    send_user();
    send_nick();

    auto channel_str = m_config->read_entry("Connection", "AutoJoinChannels", "#test");
    dbgprintf("IRCClient: Channels to autojoin: %s\n", channel_str.characters());
    auto channels = channel_str.split(',');
    for (auto& channel : channels) {
        join_channel(channel);
        dbgprintf("IRCClient: Auto joining channel: %s\n", channel.characters());
    }
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
                printf("IRCClient: Connection closed!\n");
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

    for (int i = 0; i < line.size(); ++i) {
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
    if (!m_socket->send(ByteBuffer::wrap(text.characters(), text.length()))) {
        perror("send");
        exit(1);
    }
}

void IRCClient::send_user()
{
    send(String::format("USER %s 0 * :%s\r\n", m_nickname.characters(), m_nickname.characters()));
}

void IRCClient::send_nick()
{
    send(String::format("NICK %s\r\n", m_nickname.characters()));
}

void IRCClient::send_pong(const String& server)
{
    send(String::format("PONG %s\r\n", server.characters()));
    sleep(1);
}

void IRCClient::join_channel(const String& channel_name)
{
    send(String::format("JOIN %s\r\n", channel_name.characters()));
}

void IRCClient::part_channel(const String& channel_name)
{
    send(String::format("PART %s\r\n", channel_name.characters()));
}

void IRCClient::send_whois(const String& nick)
{
    send(String::format("WHOIS %s\r\n", nick.characters()));
}

void IRCClient::handle(const Message& msg)
{
#ifdef IRC_DEBUG
    printf("IRCClient::execute: prefix='%s', command='%s', arguments=%d\n",
        msg.prefix.characters(),
        msg.command.characters(),
        msg.arguments.size());

    int i = 0;
    for (auto& arg : msg.arguments) {
        printf("    [%d]: %s\n", i, arg.characters());
        ++i;
    }
#endif

    bool is_numeric;
    int numeric = msg.command.to_uint(is_numeric);

    if (is_numeric) {
        switch (numeric) {
        case RPL_WHOISCHANNELS:
            return handle_rpl_whoischannels(msg);
        case RPL_ENDOFWHOIS:
            return handle_rpl_endofwhois(msg);
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
        }
    }

    if (msg.command == "PING")
        return handle_ping(msg);

    if (msg.command == "JOIN")
        return handle_join(msg);

    if (msg.command == "PART")
        return handle_part(msg);

    if (msg.command == "TOPIC")
        return handle_topic(msg);

    if (msg.command == "PRIVMSG")
        return handle_privmsg_or_notice(msg, PrivmsgOrNotice::Privmsg);

    if (msg.command == "NOTICE")
        return handle_privmsg_or_notice(msg, PrivmsgOrNotice::Notice);

    if (msg.command == "NICK")
        return handle_nick(msg);

    if (msg.arguments.size() >= 2)
        add_server_message(String::format("[%s] %s", msg.command.characters(), msg.arguments[1].characters()));
}

void IRCClient::add_server_message(const String& text, Color color)
{
    m_log->add_message(0, "", text, color);
    m_server_subwindow->did_add_message();
}

void IRCClient::send_privmsg(const String& target, const String& text)
{
    send(String::format("PRIVMSG %s :%s\r\n", target.characters(), text.characters()));
}

void IRCClient::send_notice(const String& target, const String& text)
{
    send(String::format("NOTICE %s :%s\r\n", target.characters(), text.characters()));
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

bool IRCClient::is_nick_prefix(char ch) const
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
    printf("handle_privmsg_or_notice: type='%s'%s, sender_nick='%s', target='%s'\n",
        type == PrivmsgOrNotice::Privmsg ? "privmsg" : "notice",
        is_ctcp ? " (ctcp)" : "",
        sender_nick.characters(),
        target.characters());
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

    if (is_ctcp) {
        auto ctcp_payload = msg.arguments[1].substring_view(1, msg.arguments[1].length() - 2);
        if (type == PrivmsgOrNotice::Privmsg)
            handle_ctcp_request(sender_nick, ctcp_payload);
        else
            handle_ctcp_response(sender_nick, ctcp_payload);
        StringBuilder builder;
        builder.append("(CTCP) ");
        builder.append(ctcp_payload);
        message_text = builder.to_string();
        message_color = Color::Blue;
    }

    {
        auto it = m_channels.find(target);
        if (it != m_channels.end()) {
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
    if (query)
        query->add_message(sender_prefix, sender_nick, message_text, message_color);
    else {
        add_server_message(String::format("<%s> %s", sender_nick.characters(), message_text.characters()), message_color);
    }
}

IRCQuery* IRCClient::query_with_name(const String& name)
{
    return m_queries.get(name).value_or(nullptr);
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
    if (msg.arguments.size() < 0)
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
    add_server_message(String::format("~ %s changed nickname to %s", old_nick.characters(), new_nick.characters()));
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

void IRCClient::handle_rpl_topic(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& channel_name = msg.arguments[1];
    auto& topic = msg.arguments[2];
    ensure_channel(channel_name).handle_topic({}, topic);
    // FIXME: Handle RPL_TOPICWHOTIME so we can know who set it and when.
}

void IRCClient::handle_rpl_namreply(const Message& msg)
{
    if (msg.arguments.size() < 4)
        return;
    auto& channel_name = msg.arguments[2];
    auto& channel = ensure_channel(channel_name);
    auto members = msg.arguments[3].split(' ');

    quick_sort(members.begin(), members.end(), [](auto& a, auto& b) {
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
}

void IRCClient::handle_rpl_endofwhois(const Message&)
{
    add_server_message("// End of WHOIS");
}

void IRCClient::handle_rpl_whoisoperator(const Message& msg)
{
    if (msg.arguments.size() < 2)
        return;
    auto& nick = msg.arguments[1];
    add_server_message(String::format("* %s is an IRC operator", nick.characters()));
}

void IRCClient::handle_rpl_whoisserver(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& server = msg.arguments[2];
    add_server_message(String::format("* %s is using server %s", nick.characters(), server.characters()));
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
    add_server_message(String::format("* %s is %s@%s, real name: %s",
        nick.characters(),
        username.characters(),
        host.characters(),
        realname.characters()));
}

void IRCClient::handle_rpl_whoisidle(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& secs = msg.arguments[2];
    add_server_message(String::format("* %s is %s seconds idle", nick.characters(), secs.characters()));
}

void IRCClient::handle_rpl_whoischannels(const Message& msg)
{
    if (msg.arguments.size() < 3)
        return;
    auto& nick = msg.arguments[1];
    auto& channel_list = msg.arguments[2];
    add_server_message(String::format("* %s is in channels %s", nick.characters(), channel_list.characters()));
}

void IRCClient::handle_rpl_topicwhotime(const Message& msg)
{
    if (msg.arguments.size() < 4)
        return;
    auto& channel_name = msg.arguments[1];
    auto& nick = msg.arguments[2];
    auto setat = msg.arguments[3];
    bool ok;
    time_t setat_time = setat.to_uint(ok);
    if (ok) {
        auto* tm = localtime(&setat_time);
        setat = String::format("%4u-%02u-%02u %02u:%02u:%02u",
            tm->tm_year + 1900,
            tm->tm_mon + 1,
            tm->tm_mday,
            tm->tm_hour,
            tm->tm_min,
            tm->tm_sec);
    }
    ensure_channel(channel_name).add_message(String::format("*** (set by %s at %s)", nick.characters(), setat.characters()), Color::Blue);
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
    for (int i = 0; i < m_windows.size(); ++i) {
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
        if (parts.size() >= 2)
            part_channel(parts[1]);
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
    send(String::format("NICK %s\r\n", nick.characters()));
}

void IRCClient::handle_whois_action(const String& nick)
{
    send_whois(nick);
}

void IRCClient::handle_open_query_action(const String& nick)
{
    ensure_query(nick);
}

void IRCClient::handle_change_nick_action(const String& nick)
{
    change_nick(nick);
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

void IRCClient::handle_ctcp_request(const StringView& peer, const StringView& payload)
{
    dbg() << "handle_ctcp_request: " << payload;

    if (payload == "VERSION") {
        send_ctcp_response(peer, "VERSION IRC Client [x86] / Serenity OS");
        return;
    }

    if (payload.starts_with("PING")) {
        send_ctcp_response(peer, payload);
        return;
    }
}

void IRCClient::handle_ctcp_response(const StringView& peer, const StringView& payload)
{
    dbg() << "handle_ctcp_response(" << peer << "): " << payload;
}
