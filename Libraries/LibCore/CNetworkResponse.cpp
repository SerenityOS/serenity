#include <LibCore/CNetworkResponse.h>

CNetworkResponse::CNetworkResponse(ByteBuffer&& payload)
    : m_payload(payload)
{
}

CNetworkResponse::~CNetworkResponse()
{
}
