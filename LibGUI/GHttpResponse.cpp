#include <LibGUI/GHttpResponse.h>

GHttpResponse::GHttpResponse(int code, ByteBuffer&& payload)
    : GNetworkResponse(move(payload))
    , m_code(code)
{
}

GHttpResponse::~GHttpResponse()
{
}
