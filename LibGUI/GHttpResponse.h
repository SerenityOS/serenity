#pragma once

#include <LibGUI/GNetworkResponse.h>
#include <AK/AKString.h>
#include <AK/HashMap.h>

class GHttpResponse : public GNetworkResponse {
public:
    virtual ~GHttpResponse() override;
    static Retained<GHttpResponse> create(int code, ByteBuffer&& payload)
    {
        return adopt(*new GHttpResponse(code, move(payload)));
    }

    int code() const { return m_code; }
    const HashMap<String, String>& headers() const { return m_headers; }

private:
    GHttpResponse(int code, ByteBuffer&&);

    int m_code { 0 };
    HashMap<String, String> m_headers;
};
