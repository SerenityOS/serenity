#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <pid>\n", argv[0]);
        return 0;
    }

    CEventLoop loop;

    int pid = atoi(argv[1]);

    CLocalSocket socket;

    socket.on_connected = [&] {
        dbg() << "Connected to PID " << pid;

        JsonObject request;
        request.set("type", "GetAllObjects");
        auto serialized = request.to_string();
        i32 length = serialized.length();
        socket.write((const u8*)&length, sizeof(length));
        socket.write(serialized);
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

        loop.quit(0);
    };

    auto success = socket.connect(CSocketAddress::local(String::format("/tmp/rpc.%d", pid)));
    if (!success) {
        fprintf(stderr, "Couldn't connect to PID %d\n", pid);
        return 1;
    }

    return loop.exec();
}
