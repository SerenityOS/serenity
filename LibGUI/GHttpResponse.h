#pragma once

#include <LibGUI/GNetworkResponse.h>
#include <AK/AKString.h>
#include <AK/HashMap.h>

class GHttpResponse : public GNetworkResponse {
public:
    virtual ~GHttpResponse() override;
    static Retained<GHttpResponse> create(int code, HashMap<String, String>&& headers, ByteBuffer&& payload)
    {
        return adopt(*new GHttpResponse(code, move(headers), move(payload)));
    }

    int code() const { return m_code; }
    const HashMap<String, String>& headers() const { return m_headers; }

private:
    GHttpResponse(int code, HashMap<String, String>&&, ByteBuffer&&);

    int m_code { 0 };
    HashMap<String, String> m_headers;
};
