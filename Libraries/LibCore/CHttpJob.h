#pragma once

#include <AK/HashMap.h>
#include <LibCore/CHttpRequest.h>
#include <LibCore/CNetworkJob.h>

class CTCPSocket;

class CHttpJob final : public CNetworkJob {
    C_OBJECT(CHttpJob)
public:
    explicit CHttpJob(const CHttpRequest&);
    virtual ~CHttpJob() override;

    virtual void start() override;
    virtual void shutdown() override;

private:
    void on_socket_connected();
    void finish_up();

    enum class State {
        InStatus,
        InHeaders,
        InBody,
        Finished,
    };

    CHttpRequest m_request;
    ObjectPtr<CTCPSocket> m_socket;
    State m_state { State::InStatus };
    int m_code { -1 };
    HashMap<String, String> m_headers;
    Vector<ByteBuffer> m_received_buffers;
    size_t m_received_size { 0 };
};
