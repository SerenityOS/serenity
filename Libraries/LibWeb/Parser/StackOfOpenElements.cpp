#include <LibWeb/DOM/Element.h>
#include <LibWeb/Parser/StackOfOpenElements.h>

namespace Web {

StackOfOpenElements::~StackOfOpenElements()
{
}

bool StackOfOpenElements::has_in_scope(const FlyString& tag_name) const
{
    static Vector<FlyString> list { "applet", "caption", "html", "table", "td", "th", "marquee", "object", "template" };
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& node = m_elements.at(i);
        if (node.tag_name() == tag_name)
            return true;
        if (list.contains_slow(node.tag_name()))
            return false;
    }
    ASSERT_NOT_REACHED();
}

}
