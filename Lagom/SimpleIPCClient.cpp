#include <LibCore/CEventLoop.h>
#include <LibCore/CTimer.h>
#include <LibCore/CoreIPCClient.h>
#include <stdio.h>
#include "SimpleEndpoint.h"

class SimpleIPCClient : public IPC::Client::ConnectionNG<SimpleEndpoint> {
    C_OBJECT(SimpleIPCClient)
public:
    SimpleIPCClient()
        : ConnectionNG("/tmp/simple-ipc")
    {}

    virtual void handshake() override {}

    i32 compute_sum(i32 a, i32 b, i32 c)
    {
        return send_sync<Simple::ComputeSum>(a, b, c)->sum();
    }
};

int main(int, char**)
{
    CEventLoop event_loop;

    SimpleIPCClient client;

    CTimer timer(100, [&] {
        i32 sum = client.compute_sum(1, 2, 3);
        dbg() << "Sum: " << sum;
    });

    CTimer kill_timer(5000, [&] {
        dbg() << "Timer fired, good-bye! :^)";
        event_loop.quit(0);
    });

    return event_loop.exec();
}
