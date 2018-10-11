#include "FrameBufferSDL.h"
#include "EventLoopSDL.h"
#include "RootWidget.h"
#include "Label.h"
#include "Button.h"
#include "TerminalWidget.h"
#include "WindowManager.h"
#include <cstdio>

int main(int c, char** v)
{
    FrameBufferSDL fb(800, 600);
    fb.show();

    EventLoopSDL loop;

    RootWidget w;
    fb.setRootWidget(&w);

    WindowManager::the();

    auto* l1 = new Label(&w);
    l1->setRect(Rect(100, 100, 300, 20));
    l1->setText("0123456789");

    auto* l2 = new Label(&w);
    l2->setRect(Rect(100, 120, 300, 20));
    l2->setText("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    
    auto* l3 = new Label(&w);
    l3->setRect(Rect(100, 140, 300, 20));
    l3->setText("abcdefghijklmnopqrstuvwxyz");

    auto* l4 = new Label(&w);
    l4->setRect(Rect(100, 160, 300, 20));
    l4->setText("!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~");

    auto* b = new Button(&w);
    b->setRect(Rect(10, 10, 100, 30));
    b->setCaption("Button!");

    auto* t = new TerminalWidget(&w);
    t->setWindowTitle("Console");

    return loop.exec();
}
