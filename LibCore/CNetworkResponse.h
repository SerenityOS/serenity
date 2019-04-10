#pragma once

#include <AK/Retainable.h>
#include <AK/ByteBuffer.h>

class CNetworkResponse : public Retainable<CNetworkResponse> {
public:
    virtual ~CNetworkResponse();

    bool is_error() const { return m_error; }
    const ByteBuffer& payload() const { return m_payload; }

protected:
    explicit CNetworkResponse(ByteBuffer&&);

    bool m_error { false };
    ByteBuffer m_payload;
};
