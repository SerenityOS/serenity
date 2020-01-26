#pragma once

#include <AK/String.h>
#include <AK/Types.h>

class DNSAnswer {
public:
    DNSAnswer(const String& name, u16 type, u16 class_code, u32 ttl, const String& record_data)
        : m_name(name)
        , m_type(type)
        , m_class_code(class_code)
        , m_ttl(ttl)
        , m_record_data(record_data)
    {
    }

    const String& name() const { return m_name; }
    u16 type() const { return m_type; }
    u16 class_code() const { return m_class_code; }
    u32 ttl() const { return m_ttl; }
    const String& record_data() const { return m_record_data; }

private:
    String m_name;
    u16 m_type { 0 };
    u16 m_class_code { 0 };
    u32 m_ttl { 0 };
    String m_record_data;
};
