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
#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/TCPSocket.h>
#include <LibEmail/Message.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

namespace LibEmail {

enum class ResponseStatus : char {
    Ok,  // Command succeeded
    No,  // Command failed
    Bad, // Command is unrecognized/has syntax error
    FailedToSend
};

enum class ConnectionState : char {
    // Usually default state; in this state when client hasn't
    // supplied any credentials yet
    NotAuthenticated,
    // User has logged in, but not selected anything;
    // can't do any message-related operations
    Authenticated,
    // User has successfully chosen a mailbox
    Selected
};

class IMAPClient final : public Core::Object {
    C_OBJECT(IMAPClient)
public:
    IMAPClient(StringView address, int port = 143);
    virtual ~IMAPClient();

    ResponseStatus login(StringView username, StringView password);
    ResponseStatus select_mailbox(StringView);
    ResponseStatus create_mailbox(StringView);
    ResponseStatus delete_mailbox(StringView);
    ResponseStatus rename_mailbox(StringView old_name, StringView new_name);
    Optional<Message> fetch(unsigned int sequence_id, Field);
    Optional<Message> fetch(unsigned int sequence_id, const Vector<Field>&);
    ConnectionState state() const { return m_state; }

private:
    StringBuilder next_message_id();
    bool send_command(StringView);
    String receive_response();

    RefPtr<Core::TCPSocket> m_socket;
    RefPtr<LibThread::Thread> m_receive_thread;
    Vector<String> m_message_queue;
    LibThread::Lock m_queue_lock;
    int m_message_id = 1;
    ConnectionState m_state = ConnectionState::NotAuthenticated;
};
}
