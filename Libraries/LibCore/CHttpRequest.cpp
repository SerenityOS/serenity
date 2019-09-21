#include <AK/StringBuilder.h>
#include <LibCore/CHttpJob.h>
#include <LibCore/CHttpRequest.h>

CHttpRequest::CHttpRequest()
{
}

CHttpRequest::~CHttpRequest()
{
}

RefPtr<CNetworkJob> CHttpRequest::schedule()
{
    auto job = CHttpJob::construct(*this);
    job->start();
    return job;
}

String CHttpRequest::method_name() const
{
    switch (m_method) {
    case Method::GET:
        return "GET";
    case Method::HEAD:
        return "HEAD";
    case Method::POST:
        return "POST";
    default:
        ASSERT_NOT_REACHED();
    }
}

ByteBuffer CHttpRequest::to_raw_request() const
{
    StringBuilder builder;
    builder.append(method_name());
    builder.append(' ');
    builder.append(m_url.path());
    builder.append(" HTTP/1.0\r\nHost: ");
    builder.append(m_url.host());
    builder.append("\r\n\r\n");
    return builder.to_byte_buffer();
}
