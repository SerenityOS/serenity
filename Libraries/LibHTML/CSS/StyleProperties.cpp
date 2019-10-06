#include <LibCore/CDirIterator.h>
#include <LibHTML/CSS/StyleProperties.h>
#include <ctype.h>

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

String StyleProperties::string_or_fallback(const StringView& property_name, const StringView& fallback) const
{
    auto value = property(property_name);
    if (!value.has_value())
        return fallback;
    return value.value()->to_string();
}

Color StyleProperties::color_or_fallback(const StringView& property_name, const Document& document, Color fallback) const
{
    auto value = property(property_name);
    if (!value.has_value())
        return fallback;
    return value.value()->to_color(document);
}

void StyleProperties::load_font() const
{
    auto font_family = string_or_fallback("font-family", "Katica");
    auto font_weight = string_or_fallback("font-weight", "normal");

    String weight;
    if (font_weight == "lighter")
        weight = "Thin";
    else if (font_weight == "normal")
        weight = "";
    else if (font_weight == "bold")
        weight = "Bold";
    else
        ASSERT_NOT_REACHED();

    auto look_for_file = [](const StringView& expected_name) -> String {
        // TODO: handle font sizes properly?
        CDirIterator it { "/res/fonts/", CDirIterator::Flags::SkipDots };
        while (it.has_next()) {
            String name = it.next_path();
            ASSERT(name.ends_with(".font"));
            if (!name.starts_with(expected_name))
                continue;

            // Check that a numeric size immediately
            // follows the font name. This prevents,
            // for example, matching KaticaBold when
            // the regular Katica is requested.
            if (!isdigit(name[expected_name.length()]))
                continue;

            return name;
        }
        return {};
    };

    String file_name = look_for_file(String::format("%s%s", font_family.characters(), weight.characters()));
    if (file_name.is_null() && weight == "")
        file_name = look_for_file(String::format("%sRegular", font_family.characters()));

    if (file_name.is_null()) {
        dbg() << "Failed to find a font for family " << font_family << " weight " << font_weight;
        ASSERT_NOT_REACHED();
    }

#ifdef HTML_DEBUG
    dbg() << "Found font " << file_name << " for family " << font_family << " weight " << font_weight;
#endif

    m_font = Font::load_from_file(String::format("/res/fonts/%s", file_name.characters()));
}
