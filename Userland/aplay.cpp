#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CEventLoop.h>
#include <cstdio>

int main(int argc, char** argv)
{
    CEventLoop loop;
    if (argc < 2) {
        fprintf(stderr, "Need a WAV to play\n");
        return 1;
    }

    AClientConnection a_conn;
    a_conn.handshake();
    AWavLoader loader(argv[1]);

    printf("\033[34;1m Playing\033[0m: %s\n", argv[1]);
    printf("\033[34;1m  Format\033[0m: %u Hz, %u-bit, %s\n",
        loader.sample_rate(),
        loader.bits_per_sample(),
        loader.num_channels() == 1 ? "Mono" : "Stereo");
    printf("\033[34;1mProgress\033[0m: \033[s");
    for (;;) {
        auto samples = loader.get_more_samples();
        if (!samples)
            break;
        printf("\033[u");
        printf("%d/%d", loader.loaded_samples(), loader.total_samples());
        fflush(stdout);
        a_conn.enqueue(*samples);
    }
    printf("\n");
    return 0;
}
