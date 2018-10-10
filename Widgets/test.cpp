#include "FrameBufferSDL.h"
#include "EventLoopSDL.h"
#include "RootWidget.h"
#include "Label.h"
#include <cstdio>

int main(int c, char** v)
{
    FrameBufferSDL fb(800, 600);
    fb.show();

    EventLoopSDL loop;

    RootWidget w;
    fb.setRootWidget(&w);

    Label l(&w);
    l.setRect(Rect(100, 100, 300, 100));
    l.setText("ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]");

    return loop.exec();
}
