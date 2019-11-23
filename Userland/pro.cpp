#include <LibCore/CEventLoop.h>
#include <LibProtocol/Client.h>
#include <LibC/SharedBuffer.h>
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

    protocol_client->on_download_finish = [&](i32 download_id, bool success, u32 total_size, i32 shared_buffer_id) {
        printf("download %d finished, success=%u, shared_buffer_id=%d\n", download_id, success, shared_buffer_id);
        if (success) {
            ASSERT(shared_buffer_id != -1);
            auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(shared_buffer_id);
            auto payload_bytes = ByteBuffer::wrap(shared_buffer->data(), total_size);
            write(STDOUT_FILENO, payload_bytes.data(), payload_bytes.size());
        }
        loop.quit(0);
    };

    protocol_client->on_download_progress = [&](i32 download_id, u32 total_size, u32 downloaded_size) {
        printf("download %d progress: %u / %u\n", download_id, downloaded_size, total_size);
    };

    i32 download_id = protocol_client->start_download("http://192.168.1.21/");
    printf("started download with id %d\n", download_id);

    return loop.exec();
}
