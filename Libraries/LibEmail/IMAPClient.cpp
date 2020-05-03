/*
 * Copyright (c) 2020, Cole Blakley <cblakley15@gmail.com>
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
#include <AK/StringBuilder.h>
#include <LibEmail/IMAPClient.h>
#include <ctype.h>

namespace LibEmail {

static ResponseStatus get_response_status(StringView response)
{
    if (response.is_empty())
        return ResponseStatus::Bad;

    const auto right_before_status = response.find_first_of(' ');
    if (!right_before_status.has_value())
        return ResponseStatus::Bad;

    if (response.substring_view(right_before_status.value() + 1, 2) == "OK") {
        return ResponseStatus::Ok;
    } else if (response.substring_view(right_before_status.value() + 1, 2) == "NO") {
        return ResponseStatus::No;
    } else if (response.substring_view(right_before_status.value() + 1, 3) == "BAD") {
        return ResponseStatus::Bad;
    } else {
        return ResponseStatus::FailedToSend;
    }
}

static String get_message_field_string(Field value)
{
    switch (value) {
    case Field::InternalDate:
        return "INTERNALDATE";
    case Field::Flags:
        return "FLAGS";
    case Field::UID:
        return "UID";
    case Field::Envelope:
        return "ENVELOPE";
    case Field::BodyText:
        return "BODY[TEXT]";
    }
    return "";
}

IMAPClient::IMAPClient(StringView address, int port)
    : m_socket{ Core::TCPSocket::construct() }
{
    bool connected = m_socket->connect(address, port);
    dbg() << "Step 1: connection: ";
    if (connected)
        dbg() << "Connected successfully";
    else
        dbg() << "Failed to connect";

    // Setup thread for receiving/queuing server responses
    m_receive_thread = LibThread::Thread::construct(
        [this] {
            while (true) {
                const StringBuilder response{ m_socket->receive(1000) };
                if (response.is_empty()) {
                    // For some reason servers sometimes send empty messages
                    continue;
                }
                m_queue_lock.lock();
                m_message_queue.append(response.to_string());
                m_queue_lock.unlock();
            }
            return 0;
        });
    m_receive_thread->start();
}

IMAPClient::~IMAPClient()
{
    m_receive_thread->quit();
}

ResponseStatus IMAPClient::login(StringView username, StringView password)
{
    StringBuilder command;
    command.append("login ");
    command.append(username);
    command.append(" ");
    command.append(password);
    bool status = send_command(command.string_view());
    if (!status) {
        dbg() << "Failed to send LOGIN command";
        return ResponseStatus::FailedToSend;
    }
    String response{ receive_response() };
    dbg() << "Step 2: Login: \n";
    const auto res_status = get_response_status(response.view());
    if (res_status == ResponseStatus::Ok) {
        m_state = ConnectionState::Authenticated;
        dbg() << "Logged in successfully";
    } else {
        dbg() << "Failed to login";
    }
    return res_status;
}

ResponseStatus IMAPClient::select_mailbox(StringView mailbox)
{
    if (m_state == ConnectionState::NotAuthenticated) {
        dbg() << "Cannot select: improper current state";
        return ResponseStatus::FailedToSend;
    }
    StringBuilder command;
    command.append("select ");
    command.append(mailbox);
    bool status = send_command(command.string_view());
    if (!status) {
        dbg() << "Failed to send SELECT command";
        return ResponseStatus::FailedToSend;
    }

    String response{ receive_response() };
    dbg() << "Step 3: Selected mailbox: " << mailbox;
    const auto res_status = get_response_status(response.view());
    if (res_status == ResponseStatus::Ok) {
        m_state = ConnectionState::Selected;
        dbg() << "Selected mailbox successfully";
    } else {
        dbg() << "Failed to select mailbox";
        dbg() << response;
    }
    return res_status;
}

ResponseStatus IMAPClient::create_mailbox(StringView mailbox)
{
    if (m_state == ConnectionState::NotAuthenticated) {
        dbg() << "Cannot create mailbox: improper current state";
        return ResponseStatus::FailedToSend;
    }

    StringBuilder command;
    command.append("create ");
    command.append(mailbox);
    bool status = send_command(command.string_view());
    if (!status) {
        dbg() << "Failed to send CREATE command";
        return ResponseStatus::FailedToSend;
    }

    String response{ receive_response() };
    return get_response_status(response.view());
}

ResponseStatus IMAPClient::delete_mailbox(StringView mailbox)
{
    if (m_state == ConnectionState::NotAuthenticated) {
        dbg() << "Cannot delete mailbox: improper current state";
        return ResponseStatus::FailedToSend;
    }

    StringBuilder command;
    command.append("delete ");
    command.append(mailbox);
    bool status = send_command(command.string_view());
    if (!status) {
        dbg() << "Failed to send DELETE command";
        return ResponseStatus::FailedToSend;
    }

    String response{ receive_response() };
    return get_response_status(response.view());
}

ResponseStatus IMAPClient::rename_mailbox(StringView old_name, StringView new_name)
{
    if (m_state == ConnectionState::NotAuthenticated) {
        dbg() << "Cannot rename mailbox: improper current state";
        return ResponseStatus::FailedToSend;
    }

    StringBuilder command;
    command.append("rename ");
    command.append(old_name);
    command.append(" ");
    command.append(new_name);
    bool status = send_command(command.string_view());
    if (!status) {
        dbg() << "Failed to send RENAME command";
        return ResponseStatus::FailedToSend;
    }

    String response{ receive_response() };
    return get_response_status(response.view());
}

Optional<Message> IMAPClient::fetch(unsigned int sequence_id, Field parameter)
{
    const Vector<Field> parameter_list{ parameter };
    return fetch(sequence_id, parameter_list);
}

Optional<Message> IMAPClient::fetch(unsigned int sequence_id,
    const Vector<Field>& parameter_list)
{
    if (m_state != ConnectionState::Selected) {
        dbg() << "Cannot fetch: improper current state";
        return {};
    }

    StringBuilder command;
    command.append("fetch ");
    command.append(String::number(sequence_id));
    command.append(" (");
    for (auto field : parameter_list) {
        command.append(get_message_field_string(field));
        command.append(" ");
    }
    if (!parameter_list.is_empty()) {
        command.trim(1);
        command.append(')');
    }

    bool status = send_command(command.string_view());
    if (!status) {
        dbg() << "Fetch failed";
        return {};
    }

    String response{ receive_response() };
    /* Some servers like to give a status message before giving the
       fetched message. If there is a status message, ignore it, use the
       next response (which will contain the actual fetched message).
       Status message format: "* 2 EXISTS/RECENT/etc. ....some more stuff".*/
    if (get_response_status(response.view()) == ResponseStatus::FailedToSend) {
        response = receive_response();
    }

    // Fetch response format: "*[gap0]2[gap1]FETCH[gap2]rest of response data...".
    int gaps[] = { -1, -1, -1 };
    int curr_gap = 0;
    for (unsigned int i = 0; i < response.length(); ++i) {
        if (response[i] == ' ') {
            gaps[curr_gap++] = i;
            if (curr_gap >= 3)
                break;
        }
    }
    if (gaps[0] == -1 || gaps[1] == -1 || gaps[2] == -1) {
        dbg() << "Fetch response is in unexpected format";
        return {};
    } else if (response.substring_view(gaps[1] + 1, gaps[2] - gaps[1] - 1) == "FETCH"
        && response.substring_view(gaps[2] + 1, 9) != "completed") {
        dbg() << "Fetched message successfully";
        dbg() << "Response: " << response;
        // Strip the "0 FETCH" part out of the response message.
        return { Message::create_from_imap_data(response.substring_view(gaps[2] + 2, response.length())) };
    } else {
        dbg() << "No message found using given sequence: " << sequence_id;
        dbg() << "Response: " << response;
        return {};
    }
}

StringBuilder IMAPClient::next_message_id()
{
    StringBuilder message;
    message.appendf("a%03d", m_message_id);
    ++m_message_id;
    return message;
}

bool IMAPClient::send_command(StringView command)
{
    StringBuilder message{ next_message_id() };
    message.append(" ");
    message.append(command);
    message.append("\r\n");
    bool status = m_socket->send(message.to_byte_buffer());
    dbg() << "Sent: " << command.to_string();
    if (!status) {
        dbg() << "Failed to send command: " << command;
    }
    return status;
}

String IMAPClient::receive_response()
{
    String response;
    while (true) {
        m_queue_lock.lock();
        if (!m_message_queue.is_empty()) {
            response = m_message_queue.take_first();
            m_queue_lock.unlock();
            return response;
        }
        m_queue_lock.unlock();
    }
    // Should never reach here.
    return response;
}
}
