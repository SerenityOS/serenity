#pragma once

#include <AK/Function.h>
#include <LibCore/CObject.h>

class CNetworkResponse;

class CNetworkJob : public CObject {
    C_OBJECT(CNetworkJob)
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

protected:
    CNetworkJob();
    void did_finish(NonnullRefPtr<CNetworkResponse>&&);
    void did_fail(Error);

private:
    RefPtr<CNetworkResponse> m_response;
    Error m_error { Error::None };
};

const char* to_string(CNetworkJob::Error);
