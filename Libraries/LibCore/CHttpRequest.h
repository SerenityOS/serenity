#pragma once

#include <AK/AKString.h>
#include <AK/URL.h>

class CNetworkJob;

class CHttpRequest {
public:
    enum Method {
        Invalid,
        HEAD,
        GET,
        POST
    };

    CHttpRequest();
    ~CHttpRequest();

    const URL& url() const { return m_url; }
    void set_url(const URL& url) { m_url = url; }

    Method method() const { return m_method; }
    void set_method(Method method) { m_method = method; }

    String method_name() const;
    ByteBuffer to_raw_request() const;

    CNetworkJob* schedule();

private:
    URL m_url;
    Method m_method { GET };
};
