#pragma once

#include <LibGUI/GNetworkJob.h>
#include <LibGUI/GHttpRequest.h>

class GTCPSocket;

class GHttpNetworkJob final : public GNetworkJob {
public:
    explicit GHttpNetworkJob(const GHttpRequest&);
    virtual ~GHttpNetworkJob() override;

    virtual void start() override;

    virtual const char* class_name() const override { return "GHttpNetworkJob"; }

private:
    GHttpRequest m_request;
    GTCPSocket* m_socket { nullptr };
};
