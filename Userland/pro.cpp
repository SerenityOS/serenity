#include <AK/URL.h>
#include <LibC/SharedBuffer.h>
#include <LibCore/CEventLoop.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>
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

    auto download = protocol_client->start_download(url.to_string());
    download->on_progress = [](u32 total_size, u32 downloaded_size) {
        dbgprintf("download progress: %u / %u\n", downloaded_size, total_size);
    };
    download->on_finish = [&](bool success, auto& payload, auto) {
        if (success)
            write(STDOUT_FILENO, payload.data(), payload.size());
        else
            fprintf(stderr, "Download failed :(\n");
        loop.quit(0);
    };
    dbgprintf("started download with id %d\n", download->id());

    return loop.exec();
}
