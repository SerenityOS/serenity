#pragma once

#include <AK/HashMap.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/CObject.h>

class CLocalSocket;
class CLocalServer;

class LookupServer final : public CObject {
    C_OBJECT(LookupServer)

public:
    LookupServer();

private:
    void load_etc_hosts();
    void service_client(RefPtr<CLocalSocket>);
    static String parse_dns_name(const u8* data, int& offset, int max_offset);
    Vector<String> lookup(const String& hostname, bool& did_timeout, unsigned short record_type);

    u16 get_next_id() { return ++m_next_id; }

    struct CachedLookup {
        time_t timestamp { 0 };
        unsigned short record_type { 0 };
        Vector<String> responses;
    };

    RefPtr<CLocalServer> m_local_server;
    u16 m_next_id { 0 };
    String m_dns_ip;
    HashMap<String, String> m_dns_custom_hostnames;
    HashMap<String, CachedLookup> m_lookup_cache;
};
