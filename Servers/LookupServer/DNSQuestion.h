#pragma once

#include <AK/String.h>
#include <AK/Types.h>

class DNSQuestion {
public:
    DNSQuestion(const String& name, u16 record_type, u16 class_code)
        : m_name(name)
        , m_record_type(record_type)
        , m_class_code(class_code)
    {
    }

    u16 record_type() const { return m_record_type; }
    u16 class_code() const { return m_class_code; }
    const String& name() const { return m_name; }

private:
    String m_name;
    u16 m_record_type { 0 };
    u16 m_class_code { 0 };
};
