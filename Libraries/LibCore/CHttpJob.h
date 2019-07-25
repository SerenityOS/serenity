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

private:
    void on_socket_connected();

    enum class State {
        InStatus,
        InHeaders,
        InBody,
        Finished,
    };

    CHttpRequest m_request;
    CTCPSocket* m_socket { nullptr };
    State m_state { State::InStatus };
    int m_code { -1 };
    HashMap<String, String> m_headers;
};
