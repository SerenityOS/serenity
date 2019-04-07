#include <LibGUI/GNetworkJob.h>
#include <LibGUI/GNetworkResponse.h>
#include <stdio.h>

GNetworkJob::GNetworkJob()
{
}

GNetworkJob::~GNetworkJob()
{
}

void GNetworkJob::did_finish(Retained<GNetworkResponse>&& response)
{
    m_response = move(response);
    printf("%s{%p} job did_finish!\n", class_name(), this);
    ASSERT(on_finish);
    on_finish(true);
}

void GNetworkJob::did_fail(Error error)
{
    m_error = error;
    dbgprintf("%s{%p} job did_fail! error=%u\n", class_name(), this, (unsigned)error);
    ASSERT(on_finish);
    on_finish(false);
}
