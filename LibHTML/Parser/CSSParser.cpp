#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

NonnullRefPtr<StyleSheet> parse_css(const String& css)
{
    Vector<NonnullRefPtr<StyleRule>> rules;

    enum class State {
        Free,
        InSelectorComponent,
        InPropertyName,
        InPropertyValue,
    };

    struct CurrentRule {
        Vector<Selector> selectors;
        Vector<StyleDeclaration> declarations;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    int index = 0;

    auto peek = [&]() -> char {
        if (index + 1 < css.length())
            return css[index + 1];
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

        Vector<Selector::Component> components;
        components.append({ type, String::copy(buffer) });
        buffer.clear();
        current_rule.selectors.append(Selector(move(components)));
    };

    auto parse_selector_list = [&] {
        for (;;) {
            parse_selector();
            if (peek() == ',')
                continue;
            if (peek() == '{')
                break;
        }
    };

    auto parse_declaration = [&] {
        consume_whitespace();
    };

    auto parse_declarations = [&] {
        for (;;) {
            parse_declaration();
            if (peek() == '}')
                break;
        }
    };

    auto parse_rule = [&] {
        parse_selector_list();
        consume_specific('{');
        parse_declarations();
        consume_specific('}');
    };

    while (index < css.length()) {
        parse_rule();
    }

    return StyleSheet::create(move(rules));
}
