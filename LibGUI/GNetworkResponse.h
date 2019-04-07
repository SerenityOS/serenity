#pragma once

#include <AK/Retainable.h>
#include <AK/ByteBuffer.h>

class GNetworkResponse : public Retainable<GNetworkResponse> {
public:
    virtual ~GNetworkResponse();

    bool is_error() const { return m_error; }
    const ByteBuffer& payload() const { return m_payload; }

protected:
    explicit GNetworkResponse(ByteBuffer&&);

    bool m_error { false };
    ByteBuffer m_payload;
};
