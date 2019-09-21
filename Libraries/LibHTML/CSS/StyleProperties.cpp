#include <LibHTML/CSS/StyleProperties.h>

void StyleProperties::set_property(const String& name, NonnullRefPtr<StyleValue> value)
{
    m_property_values.set(name, move(value));
}

Optional<NonnullRefPtr<StyleValue>> StyleProperties::property(const String& name) const
{
    auto it = m_property_values.find(name);
    if (it == m_property_values.end())
        return {};
    return it->value;
}

Length StyleProperties::length_or_fallback(const StringView& property_name, const Length& fallback) const
{
    auto value = property(property_name);
    if (!value.has_value())
        return fallback;
    return value.value()->to_length();
}
