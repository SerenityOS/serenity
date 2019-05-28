#pragma once

#include <AK/NetworkOrdered.h>
#include <AK/Types.h>

class [[gnu::packed]] DNSRecord
{
public:
    DNSRecord() {}

    word name() const { return m_name; }
    word type() const { return m_type; }
    word record_class() const { return m_class; }
    dword ttl() const { return m_ttl; }
    word data_length() const { return m_data_length; }

    void* data() { return this + 1; }
    const void* data() const { return this + 1; }

private:
    NetworkOrdered<word> m_name;
    NetworkOrdered<word> m_type;
    NetworkOrdered<word> m_class;
    NetworkOrdered<dword> m_ttl;
    NetworkOrdered<word> m_data_length;
};

static_assert(sizeof(DNSRecord) == 12);
