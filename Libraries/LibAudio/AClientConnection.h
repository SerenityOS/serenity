#pragma once

#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibAudio/ASAPI.h>
class ABuffer;

class AClientConnection {
public:
    AClientConnection();

    void play(const ABuffer& buffer);

private:
    bool drain_messages_from_server();
    bool wait_for_specific_event(ASAPI_ServerMessage::Type type, ASAPI_ServerMessage& event);
    bool post_message_to_server(const ASAPI_ClientMessage& message, const ByteBuffer& extra_data = {});
    ASAPI_ServerMessage sync_request(const ASAPI_ClientMessage& request, ASAPI_ServerMessage::Type response_type);

    CLocalSocket m_connection;
    CNotifier m_notifier;

    struct IncomingASMessageBundle {
        ASAPI_ServerMessage message;
        ByteBuffer extra_data;
    };
    Vector<IncomingASMessageBundle> m_unprocessed_bundles;
    int m_server_pid;
    int m_my_client_id;
};
