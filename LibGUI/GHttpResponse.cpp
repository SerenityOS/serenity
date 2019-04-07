#include <LibGUI/GHttpResponse.h>

GHttpResponse::GHttpResponse(int code, HashMap<String, String>&& headers, ByteBuffer&& payload)
    : GNetworkResponse(move(payload))
    , m_code(code)
    , m_headers(move(headers))
{
}

GHttpResponse::~GHttpResponse()
{
}
