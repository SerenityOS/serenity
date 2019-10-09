#include <LibCore/CTimer.h>
#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/HTMLBlinkElement.h>
#include <LibHTML/Layout/LayoutNode.h>

HTMLBlinkElement::HTMLBlinkElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
    , m_timer(CTimer::construct())
{
    m_timer->set_interval(500);
    m_timer->on_timeout = [this] { blink(); };
    m_timer->start();
}

HTMLBlinkElement::~HTMLBlinkElement()
{
}

void HTMLBlinkElement::blink()
{
    if (!layout_node())
        return;

    layout_node()->set_visible(!layout_node()->is_visible());
    layout_node()->set_needs_display();
}
