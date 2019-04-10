#include <LibCore/CHttpResponse.h>

CHttpResponse::CHttpResponse(int code, HashMap<String, String>&& headers, ByteBuffer&& payload)
    : CNetworkResponse(move(payload))
    , m_code(code)
    , m_headers(move(headers))
{
}

CHttpResponse::~CHttpResponse()
{
}
