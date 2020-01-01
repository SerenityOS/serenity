#include <AK/SharedBuffer.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

namespace LibProtocol {

Download::Download(Client& client, i32 download_id)
    : m_client(client.make_weak_ptr())
    , m_download_id(download_id)
{
}

bool Download::stop()
{
    return m_client->stop_download({}, *this);
}

void Download::did_finish(Badge<Client>, bool success, u32 total_size, i32 shared_buffer_id)
{
    if (!on_finish)
        return;

    ByteBuffer payload;
    RefPtr<SharedBuffer> shared_buffer;
    if (success && shared_buffer_id != -1) {
        shared_buffer = SharedBuffer::create_from_shared_buffer_id(shared_buffer_id);
        payload = ByteBuffer::wrap(shared_buffer->data(), total_size);
    }
    on_finish(success, payload, move(shared_buffer));
}

void Download::did_progress(Badge<Client>, u32 total_size, u32 downloaded_size)
{
    if (on_progress)
        on_progress(total_size, downloaded_size);
}

}
