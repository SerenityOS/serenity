#include "AbstractScreen.h"
#include "EventLoop.h"
#include "Event.h"
#include "Widget.h"
#include <AK/Assertions.h>

static AbstractScreen* s_the;

void AbstractScreen::initialize()
{
    s_the = nullptr;
}

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

