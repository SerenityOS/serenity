#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>

namespace AK {
class SharedBuffer;
}

namespace LibProtocol {

class Client;

class Download : public RefCounted<Download> {
public:
    static NonnullRefPtr<Download> create_from_id(Badge<Client>, Client& client, i32 download_id)
    {
        return adopt(*new Download(client, download_id));
    }

    int id() const { return m_download_id; }
    bool stop();

    Function<void(bool success, const ByteBuffer& payload, RefPtr<SharedBuffer> payload_storage)> on_finish;
    Function<void(u32 total_size, u32 downloaded_size)> on_progress;

    void did_finish(Badge<Client>, bool success, u32 total_size, i32 shared_buffer_id);
    void did_progress(Badge<Client>, u32 total_size, u32 downloaded_size);

private:
    explicit Download(Client&, i32 download_id);
    WeakPtr<Client> m_client;
    int m_download_id { -1 };
};

}
