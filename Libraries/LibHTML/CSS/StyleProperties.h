#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibDraw/Font.h>
#include <LibHTML/CSS/StyleValue.h>

class Color;

class StyleProperties : public RefCounted<StyleProperties> {
public:
    static NonnullRefPtr<StyleProperties> create() { return adopt(*new StyleProperties); }
    static NonnullRefPtr<StyleProperties> create(const StyleProperties& properties) {
        auto style_properties = new StyleProperties();
        properties.for_each_property([&](auto property_id, auto& property_value) {
            style_properties->set_property(property_id, property_value);
        });
        return adopt(*style_properties);
    }

    template<typename Callback>
    inline void for_each_property(Callback callback) const
    {
        for (auto& it : m_property_values)
            callback((CSS::PropertyID)it.key, *it.value);
    }

    void set_property(CSS::PropertyID, NonnullRefPtr<StyleValue> value);
    Optional<NonnullRefPtr<StyleValue>> property(CSS::PropertyID) const;

    Length length_or_fallback(CSS::PropertyID, const Length& fallback) const;
    String string_or_fallback(CSS::PropertyID, const StringView& fallback) const;
    Color color_or_fallback(CSS::PropertyID, const Document&, Color fallback) const;

    const Font& font() const
    {
        if (!m_font)
            load_font();
        return *m_font;
    }

    float line_height() const;

    bool operator==(const StyleProperties&) const;
    bool operator!=(const StyleProperties& other) const { return !(*this == other); }

private:
    HashMap<unsigned, NonnullRefPtr<StyleValue>> m_property_values;

    void load_font() const;

    mutable RefPtr<Font> m_font;
};
