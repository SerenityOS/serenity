#pragma once

#include <LibGUI/GNetworkJob.h>
#include <LibGUI/GHttpRequest.h>
#include <AK/HashMap.h>

class GTCPSocket;

class GHttpJob final : public GNetworkJob {
public:
    explicit GHttpJob(const GHttpRequest&);
    virtual ~GHttpJob() override;

    virtual void start() override;

    virtual const char* class_name() const override { return "GHttpJob"; }

private:
    enum class State {
        InStatus,
        InHeaders,
        InBody,
        Finished,
    };

    GHttpRequest m_request;
    GTCPSocket* m_socket { nullptr };
    State m_state { State::InStatus };
    int m_code { -1 };
    HashMap<String, String> m_headers;
};
