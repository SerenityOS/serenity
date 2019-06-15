#pragma once

#include <LibHTML/ParentNode.h>
#include <AK/AKString.h>

class Attribute {
public:
    Attribute(const String& name, const String& value)
        : m_name(name)
        , m_value(value)
    {
    }

private:
    String m_name;
    String m_value;
};

class Element : public ParentNode {
public:
    explicit Element(const String& tag_name);
    virtual ~Element() override;

    const String& tag_name() const { return m_tag_name; }

private:
    String m_tag_name;
    Vector<Attribute> m_attributes;
};

