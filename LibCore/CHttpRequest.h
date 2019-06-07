#pragma once

#include <AK/AKString.h>

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

    String hostname() const { return m_hostname; }
    int port() const { return m_port; }
    String path() const { return m_path; }
    Method method() const { return m_method; }

    void set_hostname(const String& hostname) { m_hostname = hostname; }
    void set_port(int port) { m_port = port; }
    void set_path(const String& path) { m_path = path; }
    void set_method(Method method) { m_method = method; }

    String method_name() const;
    ByteBuffer to_raw_request() const;

    CNetworkJob* schedule();

private:
    String m_hostname;
    String m_path;
    int m_port { 80 };
    Method m_method { GET };
};
