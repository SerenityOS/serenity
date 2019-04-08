#include <LibGUI/GHttpJob.h>
#include <LibGUI/GHttpResponse.h>
#include <LibGUI/GTCPSocket.h>
#include <stdio.h>
#include <unistd.h>

GHttpJob::GHttpJob(const GHttpRequest& request)
    : m_request(request)
{
}

GHttpJob::~GHttpJob()
{
}

void GHttpJob::start()
{
    ASSERT(!m_socket);
    m_socket = new GTCPSocket(this);
    int success = m_socket->connect(m_request.hostname(), m_request.port());
    if (!success)
        return did_fail(GNetworkJob::Error::ConnectionFailed);

    auto raw_request = m_request.to_raw_request();

    printf("raw_request:\n%s\n", raw_request.pointer());

    success = m_socket->send(raw_request);
    if (!success)
        return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::TransmissionFailed); });

    Vector<byte> buffer;
    while (m_socket->is_connected()) {
        if (m_state == State::InStatus) {
            while (!m_socket->can_read_line())
                usleep(1);
            ASSERT(m_socket->can_read_line());
            auto line = m_socket->read_line(PAGE_SIZE);
            if (line.is_null()) {
                printf("Expected HTTP status\n");
                return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::TransmissionFailed); });
            }
            auto parts = String::from_byte_buffer(line, Chomp).split(' ');
            if (parts.size() < 3) {
                printf("Expected 3-part HTTP status, got '%s'\n", line.pointer());
                return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::ProtocolFailed); });
            }
            bool ok;
            m_code = parts[1].to_uint(ok);
            if (!ok) {
                printf("Expected numeric HTTP status\n");
                return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::ProtocolFailed); });
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
                return did_fail(GNetworkJob::Error::ProtocolFailed);
            }
            auto chomped_line = String::from_byte_buffer(line, Chomp);
            if (chomped_line.is_empty()) {
                m_state = State::InBody;
                continue;
            }
            auto parts = chomped_line.split(':');
            if (parts.is_empty()) {
                printf("Expected HTTP header with key/value\n");
                return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::ProtocolFailed); });
            }
            auto name = parts[0];
            if (chomped_line.length() < name.length() + 2) {
                printf("Malformed HTTP header: '%s' (%d)\n", chomped_line.characters(), chomped_line.length());
                return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::ProtocolFailed); });
            }
            auto value = chomped_line.substring(name.length() + 2, chomped_line.length() - name.length() - 2);
            m_headers.set(name, value);
            printf("[%s] = '%s'\n", name.characters(), value.characters());
            continue;
        }
        ASSERT(m_state == State::InBody);
        auto payload = m_socket->receive(PAGE_SIZE);
        if (!payload) {
            if (m_socket->eof()) {
                m_state = State::Finished;
                break;
            }
            return deferred_invoke([this](auto&){ did_fail(GNetworkJob::Error::ProtocolFailed); });
        }
        buffer.append(payload.pointer(), payload.size());
    }

    auto response = GHttpResponse::create(m_code, move(m_headers), ByteBuffer::copy(buffer.data(), buffer.size()));
    deferred_invoke([this, response] (auto&) {
        did_finish(move(response));
    });
}
