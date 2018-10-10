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
    l.setRect(Rect(100, 100, 600, 100));
    l.setText("()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_abcdefghijklmnopqrstuvwxyz");
    //l.setText("Welcome to the Serenity Operating System");

    return loop.exec();
}
