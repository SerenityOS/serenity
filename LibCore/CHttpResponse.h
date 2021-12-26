#pragma once

#include <LibCore/CNetworkResponse.h>
#include <AK/AKString.h>
#include <AK/HashMap.h>

class CHttpResponse : public CNetworkResponse {
public:
    virtual ~CHttpResponse() override;
    static Retained<CHttpResponse> create(int code, HashMap<String, String>&& headers, ByteBuffer&& payload)
    {
        return adopt(*new CHttpResponse(code, move(headers), move(payload)));
    }

    int code() const { return m_code; }
    const HashMap<String, String>& headers() const { return m_headers; }

private:
    CHttpResponse(int code, HashMap<String, String>&&, ByteBuffer&&);

    int m_code { 0 };
    HashMap<String, String> m_headers;
};
