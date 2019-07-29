#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    CEventLoop loop;
    AClientConnection a_conn;
    a_conn.handshake();

    if (argc > 1) {
        int new_volume = atoi(argv[1]);
        a_conn.set_main_mix_volume(new_volume);
    }

    int volume = a_conn.get_main_mix_volume();
    printf("Volume: %d\n", volume);
    return 0;
}
