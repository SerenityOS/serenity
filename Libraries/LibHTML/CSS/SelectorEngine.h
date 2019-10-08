#pragma once

#include <LibHTML/CSS/Selector.h>

class Element;

namespace SelectorEngine {

bool matches(const Selector&, const Element&);

}
