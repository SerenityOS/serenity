#include <LibCore/CHttpJob.h>
#include <LibCore/CHttpResponse.h>
#include <ProtocolServer/HttpDownload.h>

HttpDownload::HttpDownload(PSClientConnection& client, NonnullRefPtr<CHttpJob>&& job)
    : Download(client)
    , m_job(job)
{
    m_job->on_finish = [this](bool success) {
        if (m_job->response())
            set_payload(m_job->response()->payload());
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
