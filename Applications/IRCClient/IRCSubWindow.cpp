#include "IRCSubWindow.h"

IRCSubWindow::IRCSubWindow(const String& name, GWidget* parent)
    : GWidget(parent)
    , m_name(name)
{
}

IRCSubWindow::~IRCSubWindow()
{
}
