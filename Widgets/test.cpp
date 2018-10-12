#include "FrameBufferSDL.h"
#include "EventLoopSDL.h"
#include "RootWidget.h"
#include "Label.h"
#include "Button.h"
#include "TerminalWidget.h"
#include "WindowManager.h"
#include "Window.h"
#include "ClockWidget.h"
#include <cstdio>

int main(int c, char** v)
{
    FrameBufferSDL fb(800, 600);
    fb.show();

    EventLoopSDL loop;

    RootWidget w;
    WindowManager::the().setRootWidget(&w);

    auto* fontTestWindow = new Window;
    fontTestWindow->setTitle("Font test");
    fontTestWindow->setRect({ 100, 100, 300, 80 });

    auto* fontTestWindowWidget = new Widget;
    fontTestWindow->setMainWidget(fontTestWindowWidget);
    fontTestWindowWidget->setRect({ 0, 0, 300, 80 });

    auto* l1 = new Label(fontTestWindowWidget);
    l1->setRect({ 0, 0, 300, 20 });
    l1->setText("0123456789");

    auto* l2 = new Label(fontTestWindowWidget);
    l2->setRect({ 0, 20, 300, 20 });
    l2->setText("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    
    auto* l3 = new Label(fontTestWindowWidget);
    l3->setRect({ 0, 40, 300, 20 });
    l3->setText("abcdefghijklmnopqrstuvwxyz");

    auto* l4 = new Label(fontTestWindowWidget);
    l4->setRect({ 0, 60, 300, 20 });
    l4->setText("!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~");

    auto* b = new Button(&w);
    b->setRect({ 10, 10, 100, 30 });
    b->setCaption("Button!");

    auto* win = new Window;
    win->setTitle("Console");
    win->setRect({ 100, 300, 644, 254 });

    auto* t = new TerminalWidget(nullptr);
    win->setMainWidget(t);

    auto* clockWin = new Window;
    clockWin->setTitle("Clock");
    clockWin->setRect({ 500, 50, 100, 40 });
    clockWin->setMainWidget(new ClockWidget);

    return loop.exec();
}
