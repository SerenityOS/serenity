#pragma once

#include <LibGUI/GObject.h>
#include <AK/Function.h>

class GNetworkResponse;

class GNetworkJob : public GObject {
public:
    enum class Error {
        None,
        ConnectionFailed,
        TransmissionFailed,
        ProtocolFailed,
    };
    virtual ~GNetworkJob() override;

    Function<void(bool success)> on_finish;

    bool has_error() const { return m_error != Error::None; }
    Error error() const { return m_error; }
    GNetworkResponse* response() { return m_response.ptr(); }
    const GNetworkResponse* response() const { return m_response.ptr(); }

    virtual void start() = 0;

    virtual const char* class_name() const override { return "GNetworkJob"; }

protected:
    GNetworkJob();
    void did_finish(Retained<GNetworkResponse>&&);
    void did_fail(Error);

private:
    RetainPtr<GNetworkResponse> m_response;
    Error m_error { Error::None };
};
