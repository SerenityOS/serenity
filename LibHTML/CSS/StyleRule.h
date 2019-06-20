#pragma once

#include <AK/Vector.h>
#include <LibHTML/CSS/Selector.h>
#include <LibHTML/CSS/StyleDeclaration.h>

class StyleRule {
public:
    StyleRule();
    ~StyleRule();

private:
    Vector<Selector> m_selectors;
    Vector<StyleDeclaration> m_declarations;
};
