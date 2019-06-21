#pragma once

#include <AK/AKString.h>
#include <LibHTML/CSS/StyleValue.h>

class StyleDeclaration : public RefCounted<StyleDeclaration> {
public:
    NonnullRefPtr<StyleDeclaration> create(const String& property_name, NonnullRefPtr<StyleValue>&& value)
    {
        return adopt(*new StyleDeclaration(property_name, move(value)));
    }

    ~StyleDeclaration();

    const String& property_name() const { return m_property_name; }
    const StyleValue& value() const { return *m_value; }

public:
    StyleDeclaration(const String& property_name, NonnullRefPtr<StyleValue>&&);

    String m_property_name;
    NonnullRefPtr<StyleValue> m_value;
};
