#pragma once

#include <LibCore/CObject.h>
#include <AK/Function.h>

class CNetworkResponse;

class CNetworkJob : public CObject {
public:
    enum class Error {
        None,
        ConnectionFailed,
        TransmissionFailed,
        ProtocolFailed,
    };
    virtual ~CNetworkJob() override;

    Function<void(bool success)> on_finish;

    bool has_error() const { return m_error != Error::None; }
    Error error() const { return m_error; }
    CNetworkResponse* response() { return m_response.ptr(); }
    const CNetworkResponse* response() const { return m_response.ptr(); }

    virtual void start() = 0;

    virtual const char* class_name() const override { return "CNetworkJob"; }

protected:
    CNetworkJob();
    void did_finish(Retained<CNetworkResponse>&&);
    void did_fail(Error);

private:
    RetainPtr<CNetworkResponse> m_response;
    Error m_error { Error::None };
};
