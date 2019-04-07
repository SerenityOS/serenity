#pragma once

#include <LibGUI/GNetworkJob.h>
#include <LibGUI/GHttpRequest.h>
#include <AK/HashMap.h>

class GTCPSocket;

class GHttpNetworkJob final : public GNetworkJob {
public:
    explicit GHttpNetworkJob(const GHttpRequest&);
    virtual ~GHttpNetworkJob() override;

    virtual void start() override;

    virtual const char* class_name() const override { return "GHttpNetworkJob"; }

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
