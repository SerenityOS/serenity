#include <LibGUI/GHttpNetworkJob.h>
#include <LibGUI/GHttpResponse.h>
#include <LibGUI/GTCPSocket.h>
#include <stdio.h>

GHttpNetworkJob::GHttpNetworkJob(const GHttpRequest& request)
    : m_request(request)
{
}

GHttpNetworkJob::~GHttpNetworkJob()
{
}

void GHttpNetworkJob::start()
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
        return did_fail(GNetworkJob::Error::TransmissionFailed);

    Vector<byte> buffer;
    while (m_socket->is_connected()) {
        auto payload = m_socket->receive(100000);
        if (!payload) {
            if (m_socket->eof())
                break;
            return did_fail(GNetworkJob::Error::TransmissionFailed);
        }
        buffer.append(payload.pointer(), payload.size());
    }

    auto response = GHttpResponse::create(1, ByteBuffer::copy(buffer.data(), buffer.size()));
    deferred_invoke([this, response] (GObject&) {
        printf("in the deferred invoke lambda\n");
        did_finish(move(response));
    });
}
