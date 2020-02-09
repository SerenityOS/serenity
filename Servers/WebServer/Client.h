#pragma once

#include <LibCore/Object.h>
#include <LibCore/TCPSocket.h>

namespace Core {
class HttpRequest;
}

namespace WebServer {

class Client final : public Core::Object {
    C_OBJECT(Client);
public:
    void start();

private:
    Client(NonnullRefPtr<Core::TCPSocket>, Core::Object* parent);

    void handle_request(ByteBuffer);
    void send_error_response(unsigned code, const StringView& message, const Core::HttpRequest&);
    void die();
    void log_response(unsigned code, const Core::HttpRequest&);

    NonnullRefPtr<Core::TCPSocket> m_socket;
};

}
