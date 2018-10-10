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
    : m_width(width)
    , m_height(height)
{
    ASSERT(!s_the);
    s_the = this;
}

AbstractScreen::~AbstractScreen()
{
}

void AbstractScreen::setRootWidget(Widget* widget)
{
    // FIXME: Should we support switching root widgets?
    ASSERT(!m_rootWidget);
    ASSERT(widget);
    
    m_rootWidget = widget;
    EventLoop::main().postEvent(m_rootWidget, make<ShowEvent>());
}
