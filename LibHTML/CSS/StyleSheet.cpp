#include <LibHTML/CSS/StyleSheet.h>

StyleSheet::StyleSheet(Vector<NonnullRefPtr<StyleRule>>&& rules)
    : m_rules(move(rules))
{
}

StyleSheet::~StyleSheet()
{
}
