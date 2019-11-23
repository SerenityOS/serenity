#include <AK/URL.h>
#include <LibC/SharedBuffer.h>
#include <LibCore/CEventLoop.h>
#include <LibProtocol/Client.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <url>\n", argv[0]);
        return 0;
    }

    String url_string = argv[1];
    URL url(url_string);
    if (!url.is_valid()) {
        fprintf(stderr, "'%s' is not a valid URL\n", url_string.characters());
        return 1;
    }

    CEventLoop loop;
    auto protocol_client = LibProtocol::Client::construct();
    protocol_client->handshake();

    protocol_client->on_download_finish = [&](i32 download_id, bool success, u32 total_size, i32 shared_buffer_id) {
        dbgprintf("download %d finished, success=%u, shared_buffer_id=%d\n", download_id, success, shared_buffer_id);
        if (success) {
            ASSERT(shared_buffer_id != -1);
            auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(shared_buffer_id);
            auto payload_bytes = ByteBuffer::wrap(shared_buffer->data(), total_size);
            write(STDOUT_FILENO, payload_bytes.data(), payload_bytes.size());
        }
        loop.quit(0);
    };

    protocol_client->on_download_progress = [&](i32 download_id, u32 total_size, u32 downloaded_size) {
        dbgprintf("download %d progress: %u / %u\n", download_id, downloaded_size, total_size);
    };

    i32 download_id = protocol_client->start_download(url.to_string());
    dbgprintf("started download with id %d\n", download_id);

    return loop.exec();
}
