#pragma once

#include <AK/Badge.h>
#include <ProtocolServer/Download.h>

class CHttpJob;
class HttpProtocol;

class HttpDownload final : public Download {
public:
    virtual ~HttpDownload() override;
    static NonnullRefPtr<HttpDownload> create_with_job(Badge<HttpProtocol>, PSClientConnection&, NonnullRefPtr<CHttpJob>&&);

private:
    explicit HttpDownload(PSClientConnection&, NonnullRefPtr<CHttpJob>&&);

    NonnullRefPtr<CHttpJob> m_job;
};
