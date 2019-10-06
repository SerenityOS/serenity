#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

#define PARSE_ASSERT(x) \
    if (!(x)) { \
        dbg() << "CSS PARSER ASSERTION FAILED: " << #x; \
        dbg() << "At character# " << index << " in CSS: _" << css << "_"; \
        ASSERT_NOT_REACHED(); \
    }

static Optional<Color> parse_css_color(const StringView& view)
{
    auto color = Color::from_string(view);
    if (color.has_value())
        return color;

    // FIXME: Parse all valid color strings :^)
    return {};
}

NonnullRefPtr<StyleValue> parse_css_value(const StringView& view)
{
    String string(view);
    bool ok;
    int as_int = string.to_int(ok);
    if (ok)
        return LengthStyleValue::create(Length(as_int, Length::Type::Absolute));
    unsigned as_uint = string.to_uint(ok);
    if (ok)
        return LengthStyleValue::create(Length(as_uint, Length::Type::Absolute));
    if (string == "inherit")
        return InheritStyleValue::create();
    if (string == "initial")
        return InitialStyleValue::create();
    if (string == "auto")
        return LengthStyleValue::create(Length());

    auto color = parse_css_color(view);
    if (color.has_value())
        return ColorStyleValue::create(color.value());

    return StringStyleValue::create(string);
}

class CSSParser {
public:
    CSSParser(const StringView& input)
        : css(input)
    {
    }

    char peek() const
    {
        if (index < css.length())
            return css[index];
        return 0;
    }

    char consume_specific(char ch)
    {
        PARSE_ASSERT(peek() == ch);
        ++index;
        return ch;
    }

    char consume_one()
    {
        return css[index++];
    };

    void consume_whitespace()
    {
        while (isspace(peek()))
            ++index;
    }

    bool is_valid_selector_char(char ch) const
    {
        return isalnum(ch) || ch == '-' || ch == '_' || ch == '(' || ch == ')' || ch == '@';
    }

    Optional<Selector::Component> parse_selector_component()
    {
        consume_whitespace();
        Selector::Component::Type type;
        Selector::Component::Relation relation = Selector::Component::Relation::None;

        if (peek() == '{')
            return {};

        if (peek() == '>') {
            relation = Selector::Component::Relation::ImmediateChild;
            consume_one();
            consume_whitespace();
        }

        if (peek() == '.') {
            type = Selector::Component::Type::Class;
            consume_one();
        } else if (peek() == '#') {
            type = Selector::Component::Type::Id;
            consume_one();
        } else {
            type = Selector::Component::Type::TagName;
        }

        while (is_valid_selector_char(peek()))
            buffer.append(consume_one());

        PARSE_ASSERT(!buffer.is_null());
        Selector::Component component { type, relation, String::copy(buffer) };
        buffer.clear();

        if (peek() == '[') {
            // FIXME: Implement attribute selectors.
            while (peek() != ']') {
                consume_one();
            }
            consume_one();
        }

        if (peek() == ':') {
            // FIXME: Implement pseudo stuff.
            consume_one();
            if (peek() == ':')
                consume_one();
            while (is_valid_selector_char(peek()))
                consume_one();
        }

        return component;
    }

    void parse_selector()
    {
        Vector<Selector::Component> components;

        for (;;) {
            auto component = parse_selector_component();
            if (component.has_value())
                components.append(component.value());
            consume_whitespace();
            if (peek() == ',' || peek() == '{')
                break;
        }

        current_rule.selectors.append(Selector(move(components)));
    };

    void parse_selector_list()
    {
        for (;;) {
            parse_selector();
            consume_whitespace();
            if (peek() == ',') {
                consume_one();
                continue;
            }
            if (peek() == '{')
                break;
        }
    }

    bool is_valid_property_name_char(char ch) const
    {
        return !isspace(ch) && ch != ':';
    }

    bool is_valid_property_value_char(char ch) const
    {
        return ch != '!' && ch != ';';
    }

    Optional<StyleProperty> parse_property()
    {
        consume_whitespace();
        if (peek() == ';') {
            consume_one();
            return {};
        }
        buffer.clear();
        while (is_valid_property_name_char(peek()))
            buffer.append(consume_one());
        auto property_name = String::copy(buffer);
        buffer.clear();
        consume_whitespace();
        consume_specific(':');
        consume_whitespace();
        while (is_valid_property_value_char(peek()))
            buffer.append(consume_one());
        auto property_value = String::copy(buffer);
        buffer.clear();
        consume_whitespace();
        bool is_important = false;
        if (peek() == '!') {
            consume_specific('!');
            consume_specific('i');
            consume_specific('m');
            consume_specific('p');
            consume_specific('o');
            consume_specific('r');
            consume_specific('t');
            consume_specific('a');
            consume_specific('n');
            consume_specific('t');
            consume_whitespace();
            is_important = true;
        }
        consume_specific(';');
        return StyleProperty { property_name, parse_css_value(property_value), is_important };
    }

    void parse_declaration()
    {
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace();
            if (peek() == '}')
                break;
        }
    }

    void parse_rule()
    {
        parse_selector_list();
        consume_specific('{');
        parse_declaration();
        consume_specific('}');
        rules.append(StyleRule::create(move(current_rule.selectors), StyleDeclaration::create(move(current_rule.properties))));
        consume_whitespace();
    }

    NonnullRefPtr<StyleSheet> parse_sheet()
    {
        while (index < css.length()) {
            parse_rule();
        }

        return StyleSheet::create(move(rules));
    }

    NonnullRefPtr<StyleDeclaration> parse_standalone_declaration()
    {
        consume_whitespace();
        for (;;) {
            parse_property();
            consume_whitespace();
            if (!peek())
                break;
        }
        return StyleDeclaration::create(move(current_rule.properties));
    }

private:
    NonnullRefPtrVector<StyleRule> rules;

    enum class State {
        Free,
        InSelectorComponent,
        InPropertyName,
        InPropertyValue,
    };

    struct CurrentRule {
        Vector<Selector> selectors;
        Vector<StyleProperty> properties;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    int index = 0;

    String css;
};

NonnullRefPtr<StyleSheet> parse_css(const String& css)
{
    CSSParser parser(css);
    return parser.parse_sheet();
}

NonnullRefPtr<StyleDeclaration> parse_css_declaration(const String& css)
{
    CSSParser parser(css);
    return parser.parse_standalone_declaration();
}
