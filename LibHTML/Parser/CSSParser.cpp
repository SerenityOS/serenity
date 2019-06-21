#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

NonnullRefPtr<StyleSheet> parse_css(const String& css)
{
    Vector<NonnullRefPtr<StyleRule>> rules;

    return StyleSheet::create(move(rules));
}
