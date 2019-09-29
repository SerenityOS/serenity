#pragma once

#include <LibHTML/DOM/ParentNode.h>
#include <AK/String.h>

class Attribute {
public:
    Attribute(const String& name, const String& value)
        : m_name(name)
        , m_value(value)
    {
    }

    const String& name() const { return m_name; }
    const String& value() const { return m_value; }

    void set_value(const String& value) { m_value = value; }

private:
    String m_name;
    String m_value;
};

class Element : public ParentNode {
public:
    Element(Document&, const String& tag_name);
    virtual ~Element() override;

    virtual String tag_name() const override { return m_tag_name; }

    String attribute(const String& name) const;
    void set_attribute(const String& name, const String& value);

    void set_attributes(Vector<Attribute>&&);

    template<typename Callback>
    void for_each_attribute(Callback callback) const
    {
        for (auto& attribute : m_attributes)
            callback(attribute.name(), attribute.value());
    }

    bool has_class(const StringView&) const;

private:
    Attribute* find_attribute(const String& name);
    const Attribute* find_attribute(const String& name) const;

    String m_tag_name;
    Vector<Attribute> m_attributes;
};

