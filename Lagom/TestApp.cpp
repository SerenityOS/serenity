#include <LibCore/CEventLoop.h>
#include <LibCore/CTimer.h>
#include <stdio.h>

int main(int, char**)
{
    CEventLoop event_loop;

    auto timer = CTimer::construct(100, [&] {
        dbg() << "Timer fired, good-bye! :^)";
        event_loop.quit(0);
    });

    return event_loop.exec();
}
