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
    AWavLoader loader(argv[1]);
    printf("Loaded WAV\n");

    for (;;) {
        auto samples = loader.get_more_samples();
        if (!samples) {
            break;
        }
        printf("Playing %d sample(s)\n", samples->sample_count());
        a_conn.enqueue(*samples);
    }

    printf("Exiting! :)\n");
    return 0;
}
