#include <LibHTML/CSS/StyleSheet.h>

StyleSheet::StyleSheet(NonnullRefPtrVector<StyleRule>&& rules)
    : m_rules(move(rules))
{
}

StyleSheet::~StyleSheet()
{
}
