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

    m_socket->on_ready_to_read = [&] {
        if (m_state == State::InStatus) {
            if (!m_socket->can_read_line())
                return;
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
            return;
        }
        if (m_state == State::InHeaders) {
            if (!m_socket->can_read_line())
                return;
            auto line = m_socket->read_line(PAGE_SIZE);
            if (line.is_null()) {
                printf("Expected HTTP header\n");
                return did_fail(CNetworkJob::Error::ProtocolFailed);
            }
            auto chomped_line = String::copy(line, Chomp);
            if (chomped_line.is_empty()) {
                m_state = State::InBody;
                return;
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
            return;
        }
        ASSERT(m_state == State::InBody);
        ASSERT(m_socket->can_read());
        auto payload = m_socket->receive(PAGE_SIZE);
        if (!payload) {
            if (m_socket->eof())
                return finish_up();
            return deferred_invoke([this](auto&) { did_fail(CNetworkJob::Error::ProtocolFailed); });
        }
        m_received_buffers.append(payload);
        m_received_size += payload.size();

        auto content_length_header = m_headers.get("Content-Length");
        if (content_length_header.has_value()) {
            bool ok;
            if (m_received_size >= content_length_header.value().to_uint(ok) && ok)
                return finish_up();
        }
    };
}

void CHttpJob::finish_up()
{
    m_state = State::Finished;
    auto flattened_buffer = ByteBuffer::create_uninitialized(m_received_size);
    u8* flat_ptr = flattened_buffer.data();
    for (auto& received_buffer : m_received_buffers) {
        memcpy(flat_ptr, received_buffer.data(), received_buffer.size());
        flat_ptr += received_buffer.size();
    }
    m_received_buffers.clear();

    auto response = CHttpResponse::create(m_code, move(m_headers), move(flattened_buffer));
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
