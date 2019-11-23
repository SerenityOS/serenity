#include <LibCore/CNetworkJob.h>
#include <LibCore/CNetworkResponse.h>
#include <stdio.h>

//#define CNETWORKJOB_DEBUG

CNetworkJob::CNetworkJob()
{
}

CNetworkJob::~CNetworkJob()
{
}

void CNetworkJob::did_finish(NonnullRefPtr<CNetworkResponse>&& response)
{
    // NOTE: We protect ourselves here, since the on_finish callback may otherwise
    //       trigger destruction of this job somehow.
    NonnullRefPtr<CNetworkJob> protector(*this);

    m_response = move(response);
#ifdef CNETWORKJOB_DEBUG
    dbg() << *this << " job did_finish!";
#endif
    ASSERT(on_finish);
    on_finish(true);
    shutdown();
}

void CNetworkJob::did_fail(Error error)
{
    // NOTE: We protect ourselves here, since the on_finish callback may otherwise
    //       trigger destruction of this job somehow.
    NonnullRefPtr<CNetworkJob> protector(*this);

    m_error = error;
#ifdef CNETWORKJOB_DEBUG
    dbgprintf("%s{%p} job did_fail! error: %u (%s)\n", class_name(), this, (unsigned)error, to_string(error));
#endif
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
