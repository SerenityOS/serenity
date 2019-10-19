#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CEventLoop.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    CEventLoop loop;
    if (argc < 2) {
        fprintf(stderr, "Need a WAV to play\n");
        return 1;
    }

    auto audio_client = AClientConnection::construct();
    audio_client->handshake();
    AWavLoader loader(argv[1]);

    printf("\033[34;1m Playing\033[0m: %s\n", argv[1]);
    printf("\033[34;1m  Format\033[0m: %u Hz, %u-bit, %s\n",
        loader.sample_rate(),
        loader.bits_per_sample(),
        loader.num_channels() == 1 ? "Mono" : "Stereo");
    printf("\033[34;1mProgress\033[0m: \033[s");
    for (;;) {
        auto samples = loader.get_more_samples();
        if (samples) {
            printf("\033[u");
            printf("%d/%d", loader.loaded_samples(), loader.total_samples());
            fflush(stdout);
            audio_client->enqueue(*samples);
        } else if (audio_client->get_remaining_samples()) {
            sleep(1);
        } else {
            break;
        }
    }
    printf("\n");
    return 0;
}
