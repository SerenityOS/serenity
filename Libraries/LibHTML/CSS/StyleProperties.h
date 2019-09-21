#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibHTML/CSS/StyleValue.h>

class StyleProperties {
public:
    template<typename Callback>
    inline void for_each_property(Callback callback) const
    {
        for (auto& it : m_property_values)
            callback(it.key, *it.value);
    }

    void set_property(const String& name, NonnullRefPtr<StyleValue> value);
    Optional<NonnullRefPtr<StyleValue>> property(const String& name) const;

    Length length_or_fallback(const StringView& property_name, const Length& fallback) const;

private:
    HashMap<String, NonnullRefPtr<StyleValue>> m_property_values;
};
