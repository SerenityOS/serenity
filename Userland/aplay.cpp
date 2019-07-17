#include <LibCore/CEventLoop.h>
#include <LibAudio/AWavLoader.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/ABuffer.h>
#include <cstdio>

int main(int argc, char **argv)
{
    CEventLoop loop;
    if (argc < 2) {
        fprintf(stderr, "Need a WAV to play\n");
        return 1;
    }

    printf("Establishing connection\n");
    AClientConnection a_conn;
    a_conn.handshake();
    printf("Established connection\n");
    AWavLoader loader;
    const auto& buffer = loader.load_wav(argv[1]);
    if (!buffer) {
        dbgprintf("Can't parse WAV: %s\n", loader.error_string());
        return 1;
    }

    printf("Playing WAV\n");
    a_conn.play(*buffer);
    printf("Exiting! :)\n");
    return 0;
}
