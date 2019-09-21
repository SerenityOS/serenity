#include <LibCore/CNetworkJob.h>
#include <LibCore/CNetworkResponse.h>
#include <stdio.h>

CNetworkJob::CNetworkJob()
{
}

CNetworkJob::~CNetworkJob()
{
}

void CNetworkJob::did_finish(NonnullRefPtr<CNetworkResponse>&& response)
{
    m_response = move(response);
    dbg() << *this << " job did_finish!";
    ASSERT(on_finish);
    on_finish(true);
    shutdown();
}

void CNetworkJob::did_fail(Error error)
{
    m_error = error;
    dbgprintf("%s{%p} job did_fail! error: %u (%s)\n", class_name(), this, (unsigned)error, to_string(error));
    ASSERT(on_finish);
    on_finish(false);
    shutdown();
}

const char* to_string(CNetworkJob::Error error)
{
    switch (error) {
    case CNetworkJob::Error::ProtocolFailed:
        return "ProtocolFailed";
    case CNetworkJob::Error::ConnectionFailed:
        return "ConnectionFailed";
    case CNetworkJob::Error::TransmissionFailed:
        return "TransmissionFailed";
    case CNetworkJob::Error::Cancelled:
        return "Cancelled";
    default:
        return "(Unknown error)";
    }
}
