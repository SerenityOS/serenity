#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibEmail/Message.h>

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

class IMAPClient {
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

    TCPSocket m_socket;
    std::thread m_receive_thread;
    Vector<String> m_message_queue;
    LibThread::Lock m_queue_lock;
    int m_message_id = 1;
    ConnectionState m_state = ConnectionState::NotAuthenticated;
};

}
