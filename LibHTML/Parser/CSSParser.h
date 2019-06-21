#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibHTML/CSS/StyleSheet.h>

NonnullRefPtr<StyleSheet> parse_css(const String&);

