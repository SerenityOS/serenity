#pragma once

#include <AK/AKString.h>
#include <LibHTML/CSS/StyleValue.h>

class StyleDeclaration {
public:
    StyleDeclaration();
    ~StyleDeclaration();

    const String& property_name() const { return m_property_name; }
    const StyleValue& value() const { return *m_value; }

public:
    String m_property_name;
    RetainPtr<StyleValue> m_value;
};
