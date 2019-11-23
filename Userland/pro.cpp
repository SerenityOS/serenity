#include <LibCore/CEventLoop.h>
#include <LibProtocol/Client.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    CEventLoop loop;
    auto protocol_client = LibProtocol::Client::construct();
    protocol_client->handshake();

    printf("supports HTTP? %u\n", protocol_client->is_supported_protocol("http"));
    printf(" supports FTP? %u\n", protocol_client->is_supported_protocol("ftp"));

    protocol_client->on_download_finish = [&](i32 download_id, bool success) {
        printf("download %d finished, success=%u\n", download_id, success);
        loop.quit(0);
    };

    protocol_client->on_download_progress = [&](i32 download_id, u32 total_size, u32 downloaded_size) {
        printf("download %d progress: %u / %u\n", download_id, downloaded_size, total_size);
    };

    i32 download_id = protocol_client->start_download("http://192.168.1.21/");
    printf("started download with id %d\n", download_id);

    return loop.exec();
}
