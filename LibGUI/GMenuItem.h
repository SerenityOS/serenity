#pragma once

#include <AK/AKString.h>

class GMenuItem {
public:
    enum Type { Invalid, Text, Separator };

    explicit GMenuItem(Type);
    GMenuItem(unsigned identifier, const String& text);
    ~GMenuItem();

    Type type() const { return m_type; }
    String text() const { return m_text; }
    unsigned identifier() const { return m_identifier; }

private:
    Type m_type { Invalid };
    unsigned m_identifier { 0 };
    String m_text;
};

