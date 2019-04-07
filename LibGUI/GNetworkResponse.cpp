#include <LibGUI/GNetworkResponse.h>

GNetworkResponse::GNetworkResponse(ByteBuffer&& payload)
    : m_payload(payload)
{
}

GNetworkResponse::~GNetworkResponse()
{
}
