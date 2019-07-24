#include <LibCore/CHttpJob.h>
#include <LibCore/CHttpResponse.h>
#include <LibCore/CTCPSocket.h>
#include <stdio.h>
#include <unistd.h>

CHttpJob::CHttpJob(const CHttpRequest& request)
    : m_request(request)
{
}

CHttpJob::~CHttpJob()
{
}

void CHttpJob::on_socket_connected()
{
    auto raw_request = m_request.to_raw_request();
#if 0
    printf("raw_request:\n%s\n", String::copy(raw_request).characters());
#endif

    bool success = m_socket->send(raw_request);
    if (!success)
        return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::TransmissionFailed); });

    Vector<u8> buffer;
    while (m_socket->is_connected()) {
        if (m_state == State::InStatus) {
            while (!m_socket->can_read_line())
                usleep(1);
            ASSERT(m_socket->can_read_line());
            auto line = m_socket->read_line(PAGE_SIZE);
            if (line.is_null()) {
                printf("Expected HTTP status\n");
                return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::TransmissionFailed); });
            }
            auto parts = String::copy(line, Chomp).split(' ');
            if (parts.size() < 3) {
                printf("Expected 3-part HTTP status, got '%s'\n", line.pointer());
                return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::ProtocolFailed); });
            }
            bool ok;
            m_code = parts[1].to_uint(ok);
            if (!ok) {
                printf("Expected numeric HTTP status\n");
                return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::ProtocolFailed); });
            }
            m_state = State::InHeaders;
            continue;
        }
        if (m_state == State::InHeaders) {
            while (!m_socket->can_read_line())
                usleep(1);
            auto line = m_socket->read_line(PAGE_SIZE);
            if (line.is_null()) {
                printf("Expected HTTP header\n");
                return did_fail(CNetworkJob::Error::ProtocolFailed);
            }
            auto chomped_line = String::copy(line, Chomp);
            if (chomped_line.is_empty()) {
                m_state = State::InBody;
                continue;
            }
            auto parts = chomped_line.split(':');
            if (parts.is_empty()) {
                printf("Expected HTTP header with key/value\n");
                return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::ProtocolFailed); });
            }
            auto name = parts[0];
            if (chomped_line.length() < name.length() + 2) {
                printf("Malformed HTTP header: '%s' (%d)\n", chomped_line.characters(), chomped_line.length());
                return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::ProtocolFailed); });
            }
            auto value = chomped_line.substring(name.length() + 2, chomped_line.length() - name.length() - 2);
            m_headers.set(name, value);
            printf("[%s] = '%s'\n", name.characters(), value.characters());
            continue;
        }
        ASSERT(m_state == State::InBody);
        while (!m_socket->can_read())
            usleep(1);
        ASSERT(m_socket->can_read());
        auto payload = m_socket->receive(PAGE_SIZE);
        if (!payload) {
            if (m_socket->eof()) {
                m_state = State::Finished;
                break;
            }
            return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::ProtocolFailed); });
        }
        buffer.append(payload.pointer(), payload.size());

        bool ok;
        if (buffer.size() >= m_headers.get("Content-Length").value_or("0").to_int(ok) && ok) {
            m_state = State::Finished;
            break;
        }
    }

    auto response = CHttpResponse::create(m_code, move(m_headers), ByteBuffer::copy(buffer.data(), buffer.size()));
    deferred_invoke([this, response](auto&) {
        did_finish(move(response));
    });
}

void CHttpJob::start()
{
    ASSERT(!m_socket);
    m_socket = new CTCPSocket(this);
    m_socket->on_connected = [this] {
        printf("Socket on_connected callback\n");
        on_socket_connected();
    };
    bool success = m_socket->connect(m_request.hostname(), m_request.port());
    if (!success)
        return did_fail(CNetworkJob::Error::ConnectionFailed);
}
