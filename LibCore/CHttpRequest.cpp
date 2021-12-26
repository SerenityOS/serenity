#include <LibCore/CHttpRequest.h>
#include <LibCore/CHttpJob.h>
#include <AK/StringBuilder.h>

CHttpRequest::CHttpRequest()
{
}

CHttpRequest::~CHttpRequest()
{
}

CNetworkJob* CHttpRequest::schedule()
{
    auto* job = new CHttpJob(*this);
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
    builder.append(m_path);
    builder.append(" HTTP/1.0\nHost: ");
    builder.append(m_hostname);
    builder.append("\n\n");
    return builder.to_byte_buffer();
}
