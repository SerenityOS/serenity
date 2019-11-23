#include <LibCore/CHttpJob.h>
#include <LibCore/CHttpRequest.h>
#include <ProtocolServer/HttpDownload.h>
#include <ProtocolServer/HttpProtocol.h>

HttpProtocol::HttpProtocol()
    : Protocol("http")
{
}

HttpProtocol::~HttpProtocol()
{
}

RefPtr<Download> HttpProtocol::start_download(PSClientConnection& client, const URL& url)
{
    CHttpRequest request;
    request.set_method(CHttpRequest::Method::GET);
    request.set_url(url);
    auto job = request.schedule();
    if (!job)
        return nullptr;
    return HttpDownload::create_with_job({}, client, (CHttpJob&)*job);
}
