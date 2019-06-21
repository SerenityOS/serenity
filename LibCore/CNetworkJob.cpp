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
    printf("%s{%p} job did_finish!\n", class_name(), this);
    ASSERT(on_finish);
    on_finish(true);
}

void CNetworkJob::did_fail(Error error)
{
    m_error = error;
    dbgprintf("%s{%p} job did_fail! error=%u\n", class_name(), this, (unsigned)error);
    ASSERT(on_finish);
    on_finish(false);
}
