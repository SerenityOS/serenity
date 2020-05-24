#include <LibWeb/DOM/Element.h>
#include <LibWeb/Parser/StackOfOpenElements.h>

namespace Web {

static Vector<FlyString> s_base_list { "applet", "caption", "html", "table", "td", "th", "marquee", "object", "template" };

StackOfOpenElements::~StackOfOpenElements()
{
}

bool StackOfOpenElements::has_in_scope_impl(const FlyString& tag_name, const Vector<FlyString> &list) const
{
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& node = m_elements.at(i);
        if (node.tag_name() == tag_name)
            return true;
        if (list.contains_slow(node.tag_name()))
            return false;
    }
    ASSERT_NOT_REACHED();
}

bool StackOfOpenElements::has_in_scope(const FlyString& tag_name) const
{
    return has_in_scope_impl(tag_name, s_base_list);
}

bool StackOfOpenElements::has_in_button_scope(const FlyString& tag_name) const
{
    auto list = s_base_list;
    list.append("button");
    return has_in_scope_impl(tag_name, list);
}

bool StackOfOpenElements::contains(const Element& element) const
{
    for (auto& element_on_stack : m_elements) {
        if (&element == &element_on_stack)
            return true;
    }
    return false;
}

}
