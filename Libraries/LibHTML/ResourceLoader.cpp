#include <LibC/SharedBuffer.h>
#include <LibCore/CFile.h>
#include <LibHTML/ResourceLoader.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

ResourceLoader& ResourceLoader::the()
{
    static ResourceLoader* s_the;
    if (!s_the)
        s_the = &ResourceLoader::construct().leak_ref();
    return *s_the;
}

ResourceLoader::ResourceLoader()
    : m_protocol_client(LibProtocol::Client::construct())
{
}

void ResourceLoader::load(const URL& url, Function<void(const ByteBuffer&)> callback)
{
    if (url.protocol() == "file") {
        auto f = CFile::construct();
        f->set_filename(url.path());
        if (!f->open(CIODevice::OpenMode::ReadOnly)) {
            dbg() << "ResourceLoader::load: Error: " << f->error_string();
            callback({});
            return;
        }

        auto data = f->read_all();
        deferred_invoke([data = move(data), callback = move(callback)](auto&) {
            callback(data);
        });
        return;
    }

    if (url.protocol() == "http") {
        auto download = protocol_client().start_download(url.to_string());
        download->on_finish = [this, callback = move(callback)](bool success, const ByteBuffer& payload, auto) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                dbg() << "HTTP load failed!";
                callback({});
                return;
            }
            callback(ByteBuffer::copy(payload.data(), payload.size()));
        };
        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();
        return;
    }

    dbg() << "Unimplemented protocol: " << url.protocol();
    ASSERT_NOT_REACHED();
}
