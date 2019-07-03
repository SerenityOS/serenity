#pragma once

#include <AK/NetworkOrdered.h>
#include <AK/Types.h>

class [[gnu::packed]] DNSRecord
{
public:
    DNSRecord() {}

    u16 name() const { return m_name; }
    u16 type() const { return m_type; }
    u16 record_class() const { return m_class; }
    u32 ttl() const { return m_ttl; }
    u16 data_length() const { return m_data_length; }

    void* data() { return this + 1; }
    const void* data() const { return this + 1; }

private:
    NetworkOrdered<u16> m_name;
    NetworkOrdered<u16> m_type;
    NetworkOrdered<u16> m_class;
    NetworkOrdered<u32> m_ttl;
    NetworkOrdered<u16> m_data_length;
};

static_assert(sizeof(DNSRecord) == 12);
