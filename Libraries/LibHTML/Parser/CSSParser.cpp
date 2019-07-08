#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

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
    return StringStyleValue::create(string);
}

NonnullRefPtr<StyleSheet> parse_css(const String& css)
{
    NonnullRefPtrVector<StyleRule> rules;

    enum class State {
        Free,
        InSelectorComponent,
        InPropertyName,
        InPropertyValue,
    };

    struct CurrentRule {
        Vector<Selector> selectors;
        NonnullRefPtrVector<StyleDeclaration> declarations;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    int index = 0;

    auto peek = [&]() -> char {
        if (index < css.length())
            return css[index];
        return 0;
    };

    auto consume_specific = [&](char ch) {
        ASSERT(peek() == ch);
        ++index;
        return ch;
    };

    auto consume_one = [&]() -> char {
        return css[index++];
    };

    auto consume_whitespace = [&] {
        while (isspace(peek()))
            ++index;
    };

    auto is_valid_selector_char = [](char ch) {
        return isalnum(ch) || ch == '-' || ch == '_';
    };

    auto parse_selector = [&] {
        consume_whitespace();
        Selector::Component::Type type;
        if (peek() == '.')
            type = Selector::Component::Type::Class;
        else if (peek() == '#')
            type = Selector::Component::Type::Id;
        else
            type = Selector::Component::Type::TagName;

        while (is_valid_selector_char(peek()))
            buffer.append(consume_one());

        ASSERT(!buffer.is_null());

        auto component_string = String::copy(buffer);

        Vector<Selector::Component> components;
        components.append({ type, component_string });
        buffer.clear();
        current_rule.selectors.append(Selector(move(components)));
    };

    auto parse_selector_list = [&] {
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
    };

    auto is_valid_property_name_char = [](char ch) {
        return !isspace(ch) && ch != ':';
    };

    auto is_valid_property_value_char = [](char ch) {
        return !isspace(ch) && ch != ';';
    };

    auto parse_declaration = [&] {
        consume_whitespace();
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
        consume_specific(';');
        current_rule.declarations.append(StyleDeclaration::create(property_name, parse_css_value(property_value)));
    };

    auto parse_declarations = [&] {
        for (;;) {
            parse_declaration();
            consume_whitespace();
            if (peek() == '}')
                break;
        }
    };

    auto parse_rule = [&] {
        parse_selector_list();
        consume_specific('{');
        parse_declarations();
        consume_specific('}');
        rules.append(StyleRule::create(move(current_rule.selectors), move(current_rule.declarations)));
    };

    while (index < css.length()) {
        parse_rule();
    }

    return StyleSheet::create(move(rules));
}
