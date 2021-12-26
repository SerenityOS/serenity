#include <LibHTML/CSS/SelectorEngine.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

StyleResolver::StyleResolver(Document& document)
    : m_document(document)
{
}

StyleResolver::~StyleResolver()
{
}

static StyleSheet& default_stylesheet()
{
    static StyleSheet* sheet;
    if (!sheet) {
        extern const char default_stylesheet_source[];
        String css = default_stylesheet_source;
        sheet = parse_css(css).leak_ref();
    }
    return *sheet;
}

template<typename Callback>
void StyleResolver::for_each_stylesheet(Callback callback) const
{
    callback(default_stylesheet());
    for (auto& sheet : document().stylesheets()) {
        callback(sheet);
    }
}

NonnullRefPtrVector<StyleRule> StyleResolver::collect_matching_rules(const Element& element) const
{
    NonnullRefPtrVector<StyleRule> matching_rules;

    for_each_stylesheet([&](auto& sheet) {
        for (auto& rule : sheet.rules()) {
            for (auto& selector : rule.selectors()) {
                if (SelectorEngine::matches(selector, element)) {
                    matching_rules.append(rule);
                    break;
                }
            }
        }
    });

#ifdef HTML_DEBUG
    dbgprintf("Rules matching Element{%p}\n", &element);
    for (auto& rule : matching_rules) {
        dump_rule(rule);
    }
#endif

    return matching_rules;
}

bool StyleResolver::is_inherited_property(CSS::PropertyID property_id)
{
    static HashTable<CSS::PropertyID> inherited_properties;
    if (inherited_properties.is_empty()) {
        inherited_properties.set(CSS::PropertyID::BorderCollapse);
        inherited_properties.set(CSS::PropertyID::BorderSpacing);
        inherited_properties.set(CSS::PropertyID::Color);
        inherited_properties.set(CSS::PropertyID::FontFamily);
        inherited_properties.set(CSS::PropertyID::FontSize);
        inherited_properties.set(CSS::PropertyID::FontStyle);
        inherited_properties.set(CSS::PropertyID::FontVariant);
        inherited_properties.set(CSS::PropertyID::FontWeight);
        inherited_properties.set(CSS::PropertyID::LetterSpacing);
        inherited_properties.set(CSS::PropertyID::LineHeight);
        inherited_properties.set(CSS::PropertyID::ListStyle);
        inherited_properties.set(CSS::PropertyID::ListStyleImage);
        inherited_properties.set(CSS::PropertyID::ListStylePosition);
        inherited_properties.set(CSS::PropertyID::ListStyleType);
        inherited_properties.set(CSS::PropertyID::TextAlign);
        inherited_properties.set(CSS::PropertyID::TextIndent);
        inherited_properties.set(CSS::PropertyID::TextTransform);
        inherited_properties.set(CSS::PropertyID::Visibility);
        inherited_properties.set(CSS::PropertyID::WhiteSpace);
        inherited_properties.set(CSS::PropertyID::WordSpacing);

        // FIXME: This property is not supposed to be inherited, but we currently
        //        rely on inheritance to propagate decorations into line boxes.
        inherited_properties.set(CSS::PropertyID::TextDecoration);
    }
    return inherited_properties.contains(property_id);
}

static Vector<String> split_on_whitespace(const StringView& string)
{
    if (string.is_empty())
        return {};

    Vector<String> v;
    int substart = 0;
    for (int i = 0; i < string.length(); ++i) {
        char ch = string.characters_without_null_termination()[i];
        if (isspace(ch)) {
            int sublen = i - substart;
            if (sublen != 0)
                v.append(string.substring_view(substart, sublen));
            substart = i + 1;
        }
    }
    int taillen = string.length() - substart;
    if (taillen != 0)
        v.append(string.substring_view(substart, taillen));
    return v;
}

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, const StyleValue& value)
{
    if (property_id == CSS::PropertyID::Margin) {
        if (value.is_length()) {
            style.set_property(CSS::PropertyID::MarginTop, value);
            style.set_property(CSS::PropertyID::MarginRight, value);
            style.set_property(CSS::PropertyID::MarginBottom, value);
            style.set_property(CSS::PropertyID::MarginLeft, value);
            return;
        }
        if (value.is_string()) {
            auto parts = split_on_whitespace(value.to_string());
            if (parts.size() == 2) {
                auto vertical = parse_css_value(parts[0]);
                auto horizontal = parse_css_value(parts[1]);
                style.set_property(CSS::PropertyID::MarginTop, vertical);
                style.set_property(CSS::PropertyID::MarginBottom, vertical);
                style.set_property(CSS::PropertyID::MarginLeft, horizontal);
                style.set_property(CSS::PropertyID::MarginRight, horizontal);
                return;
            }
            if (parts.size() == 3) {
                auto top = parse_css_value(parts[0]);
                auto horizontal = parse_css_value(parts[1]);
                auto bottom = parse_css_value(parts[2]);
                style.set_property(CSS::PropertyID::MarginTop, top);
                style.set_property(CSS::PropertyID::MarginBottom, bottom);
                style.set_property(CSS::PropertyID::MarginLeft, horizontal);
                style.set_property(CSS::PropertyID::MarginRight, horizontal);
                return;
            }
            if (parts.size() == 4) {
                auto top = parse_css_value(parts[0]);
                auto right = parse_css_value(parts[1]);
                auto bottom = parse_css_value(parts[2]);
                auto left = parse_css_value(parts[3]);
                style.set_property(CSS::PropertyID::MarginTop, top);
                style.set_property(CSS::PropertyID::MarginBottom, bottom);
                style.set_property(CSS::PropertyID::MarginLeft, left);
                style.set_property(CSS::PropertyID::MarginRight, right);
                return;
            }
            dbg() << "Unsure what to do with CSS margin value '" << value.to_string() << "'";
            return;
        }
        return;
    }

    style.set_property(property_id, value);
}

NonnullRefPtr<StyleProperties> StyleResolver::resolve_style(const Element& element, const StyleProperties* parent_style) const
{
    auto style = StyleProperties::create();

    if (parent_style) {
        parent_style->for_each_property([&](auto property_id, auto& value) {
            if (is_inherited_property(property_id))
                set_property_expanding_shorthands(style, property_id, value);
        });
    }

    element.apply_presentational_hints(*style);

    auto matching_rules = collect_matching_rules(element);
    for (auto& rule : matching_rules) {
        for (auto& property : rule.declaration().properties()) {
            set_property_expanding_shorthands(style, property.property_id, property.value);
        }
    }

    auto style_attribute = element.attribute("style");
    if (!style_attribute.is_null()) {
        if (auto declaration = parse_css_declaration(style_attribute)) {
            for (auto& property : declaration->properties()) {
                set_property_expanding_shorthands(style, property.property_id, property.value);
            }
        }
    }

    return style;
}
