#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibHTML/CSS/StyleSheet.h>

RefPtr<StyleSheet> parse_css(const StringView&);
RefPtr<StyleDeclaration> parse_css_declaration(const StringView&);

