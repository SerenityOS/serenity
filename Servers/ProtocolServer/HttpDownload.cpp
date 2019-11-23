#include <LibCore/CHttpJob.h>
#include <ProtocolServer/HttpDownload.h>

HttpDownload::HttpDownload(PSClientConnection& client, NonnullRefPtr<CHttpJob>&& job)
    : Download(client)
    , m_job(job)
{
    m_job->on_finish = [this](bool success) {
        did_finish(success);
    };
}

HttpDownload::~HttpDownload()
{
}

NonnullRefPtr<HttpDownload> HttpDownload::create_with_job(Badge<HttpProtocol>, PSClientConnection& client, NonnullRefPtr<CHttpJob>&& job)
{
    return adopt(*new HttpDownload(client, move(job)));
}
