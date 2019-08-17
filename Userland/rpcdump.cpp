#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <pid>\n", argv[0]);
        return 0;
    }

    CEventLoop loop;

    int pid = atoi(argv[1]);

    CLocalSocket socket;
    auto success = socket.connect(CSocketAddress::local(String::format("/tmp/rpc.%d", pid)));
    if (!success) {
        fprintf(stderr, "Couldn't connect to PID %d\n", pid);
        return 1;
    }

    socket.on_connected = [&] {
        dbg() << "Connected to PID " << pid;
    };

    socket.on_ready_to_read = [&] {
        if (socket.eof()) {
            dbg() << "Disconnected from PID " << pid;
            loop.quit(0);
            return;
        }

        auto data = socket.read_all();

        for (int i = 0; i < data.size(); ++i)
            putchar(data[i]);
        printf("\n");
    };

    return loop.exec();
}
