#include <AK/HashMap.h>
#include <LibHTML/CSS/PropertyID.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

#define PARSE_ASSERT(x)                                                   \
    if (!(x)) {                                                           \
        dbg() << "CSS PARSER ASSERTION FAILED: " << #x;                   \
        dbg() << "At character# " << index << " in CSS: _" << css << "_"; \
        ASSERT_NOT_REACHED();                                             \
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
    char* endptr = nullptr;
    long value = strtol(String(view).characters(), &endptr, 10);
    if (endptr && ((!*endptr) || (endptr[0] == 'p' && endptr[1] == 'x' && endptr[2] == '\0')))
        return LengthStyleValue::create(Length((float)value, Length::Type::Absolute));
    if (string == "inherit")
        return InheritStyleValue::create();
    if (string == "initial")
        return InitialStyleValue::create();
    if (string == "auto")
        return LengthStyleValue::create(Length());

    auto color = parse_css_color(view);
    if (color.has_value())
        return ColorStyleValue::create(color.value());

    if (string == "-libhtml-link")
        return IdentifierStyleValue::create(CSS::ValueID::VendorSpecificLink);

    return StringStyleValue::create(string);
}

class CSSParser {
public:
    CSSParser(const StringView& input)
        : css(input)
    {
    }

    bool next_is(const char* str) const
    {
        int len = strlen(str);
        for (int i = 0; i < len; ++i) {
            if (peek(i) != str[i])
                return false;
        }
        return true;
    }

    char peek(int offset = 0) const
    {
        if ((index + offset) < css.length())
            return css[index + offset];
        return 0;
    }

    char consume_specific(char ch)
    {
        if (peek() != ch) {
            dbg() << "peek() != '" << ch << "'";
        }
        PARSE_ASSERT(peek() == ch);
        PARSE_ASSERT(index < css.length());
        ++index;
        return ch;
    }

    char consume_one()
    {
        PARSE_ASSERT(index < css.length());
        return css[index++];
    };

    void consume_whitespace_or_comments()
    {
        bool in_comment = false;
        for (; index < css.length(); ++index) {
            char ch = peek();
            if (isspace(ch))
                continue;
            if (!in_comment && ch == '/' && peek(1) == '*') {
                in_comment = true;
                ++index;
                continue;
            }
            if (in_comment && ch == '*' && peek(1) == '/') {
                in_comment = false;
                ++index;
                continue;
            }
            if (in_comment)
                continue;
            break;
        }
    }

    bool is_valid_selector_char(char ch) const
    {
        return isalnum(ch) || ch == '-' || ch == '_' || ch == '(' || ch == ')' || ch == '@';
    }

    bool is_combinator(char ch) const
    {
        return ch == '~' || ch == '>' || ch == '+';
    }

    Optional<Selector::Component> parse_selector_component()
    {
        consume_whitespace_or_comments();
        Selector::Component::Type type;
        Selector::Component::Relation relation = Selector::Component::Relation::Descendant;

        if (peek() == '{')
            return {};

        if (is_combinator(peek())) {
            switch (peek()) {
            case '>':
                relation = Selector::Component::Relation::ImmediateChild;
                break;
            case '+':
                relation = Selector::Component::Relation::AdjacentSibling;
                break;
            case '~':
                relation = Selector::Component::Relation::GeneralSibling;
                break;
            }
            consume_one();
            consume_whitespace_or_comments();
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
        Selector::Component component { type, Selector::Component::PseudoClass::None, relation, String::copy(buffer) };
        buffer.clear();

        if (peek() == '[') {
            // FIXME: Implement attribute selectors.
            while (peek() != ']') {
                consume_one();
            }
            consume_one();
        }

        if (peek() == ':') {
            // FIXME: Implement pseudo elements.
            [[maybe_unused]] bool is_pseudo_element = false;
            consume_one();
            if (peek() == ':') {
                is_pseudo_element = true;
                consume_one();
            }
            while (is_valid_selector_char(peek()))
                buffer.append(consume_one());

            auto pseudo_name = String::copy(buffer);
            buffer.clear();

            if (pseudo_name == "link")
                component.pseudo_class = Selector::Component::PseudoClass::Link;
            else if (pseudo_name == "hover")
                component.pseudo_class = Selector::Component::PseudoClass::Hover;
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
            consume_whitespace_or_comments();
            if (peek() == ',' || peek() == '{')
                break;
        }

        if (components.is_empty())
            return;
        components.first().relation = Selector::Component::Relation::None;

        current_rule.selectors.append(Selector(move(components)));
    };

    void parse_selector_list()
    {
        for (;;) {
            parse_selector();
            consume_whitespace_or_comments();
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
        return ch && !isspace(ch) && ch != ':';
    }

    bool is_valid_property_value_char(char ch) const
    {
        return ch && ch != '!' && ch != ';' && ch != '}';
    }

    Optional<StyleProperty> parse_property()
    {
        consume_whitespace_or_comments();
        if (peek() == ';') {
            consume_one();
            return {};
        }
        buffer.clear();
        while (is_valid_property_name_char(peek()))
            buffer.append(consume_one());
        auto property_name = String::copy(buffer);
        buffer.clear();
        consume_whitespace_or_comments();
        consume_specific(':');
        consume_whitespace_or_comments();
        while (is_valid_property_value_char(peek()))
            buffer.append(consume_one());

        // Remove trailing whitespace.
        while (!buffer.is_empty() && isspace(buffer.last()))
            buffer.take_last();

        auto property_value = String::copy(buffer);
        buffer.clear();
        consume_whitespace_or_comments();
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
            consume_whitespace_or_comments();
            is_important = true;
        }
        if (peek() && peek() != '}')
            consume_specific(';');

        auto property_id = CSS::property_id_from_string(property_name);
        return StyleProperty { property_id, parse_css_value(property_value), is_important };
    }

    void parse_declaration()
    {
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (peek() == '}')
                break;
        }
    }

    void parse_rule()
    {
        consume_whitespace_or_comments();
        if (index >= css.length())
            return;

        // FIXME: We ignore @media rules for now.
        if (next_is("@media")) {
            while (peek() != '{')
                consume_one();
            int level = 0;
            for (;;) {
                auto ch = consume_one();
                if (ch == '{') {
                    ++level;
                } else if (ch == '}') {
                    --level;
                    if (level == 0)
                        break;
                }
            }
            consume_whitespace_or_comments();
            return;
        }

        parse_selector_list();
        consume_specific('{');
        parse_declaration();
        consume_specific('}');
        rules.append(StyleRule::create(move(current_rule.selectors), StyleDeclaration::create(move(current_rule.properties))));
        consume_whitespace_or_comments();
    }

    RefPtr<StyleSheet> parse_sheet()
    {
        while (index < css.length()) {
            parse_rule();
        }

        return StyleSheet::create(move(rules));
    }

    RefPtr<StyleDeclaration> parse_standalone_declaration()
    {
        consume_whitespace_or_comments();
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (!peek())
                break;
        }
        return StyleDeclaration::create(move(current_rule.properties));
    }

private:
    NonnullRefPtrVector<StyleRule> rules;

    struct CurrentRule {
        Vector<Selector> selectors;
        Vector<StyleProperty> properties;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    int index = 0;

    StringView css;
};

RefPtr<StyleSheet> parse_css(const StringView& css)
{
    CSSParser parser(css);
    return parser.parse_sheet();
}

RefPtr<StyleDeclaration> parse_css_declaration(const StringView& css)
{
    CSSParser parser(css);
    return parser.parse_standalone_declaration();
}
