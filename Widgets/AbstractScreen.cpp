#include "AbstractScreen.h"
#include "EventLoop.h"
#include "Event.h"
#include "Widget.h"
#include <AK/Assertions.h>

static AbstractScreen* s_the;

AbstractScreen& AbstractScreen::the()
{
    ASSERT(s_the);
    return *s_the;
}

AbstractScreen::AbstractScreen(unsigned width, unsigned height)
    : Object(nullptr)
    , m_width(width)
    , m_height(height)
{
    ASSERT(!s_the);
    s_the = this;
}

AbstractScreen::~AbstractScreen()
{
}

void AbstractScreen::event(Event& event)
{
    if (event.type() == Event::MouseMove
        || event.type() == Event::MouseDown
        || event.type() == Event::MouseUp) {
        auto& me = static_cast<MouseEvent&>(event);
        //printf("AbstractScreen::onMouseMove: %d, %d\n", me.x(), me.y());
        auto result = m_rootWidget->hitTest(me.x(), me.y());
        //printf("hit test for %d,%d found: %s{%p} %d,%d\n", me.x(), me.y(), result.widget->className(), result.widget, result.localX, result.localY);
        auto localEvent = make<MouseEvent>(event.type(), result.localX, result.localY, me.button());
        result.widget->event(*localEvent);
    }
}

void AbstractScreen::setRootWidget(Widget* widget)
{
    // FIXME: Should we support switching root widgets?
    ASSERT(!m_rootWidget);
    ASSERT(widget);
    
    m_rootWidget = widget;
    EventLoop::main().postEvent(m_rootWidget, make<ShowEvent>());
}
