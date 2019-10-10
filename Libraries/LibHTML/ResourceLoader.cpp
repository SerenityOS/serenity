#include <LibCore/CFile.h>
#include <LibCore/CHttpJob.h>
#include <LibCore/CHttpRequest.h>
#include <LibCore/CNetworkResponse.h>
#include <LibHTML/ResourceLoader.h>

ResourceLoader& ResourceLoader::the()
{
    static ResourceLoader* s_the;
    if (!s_the)
        s_the = &ResourceLoader::construct().leak_ref();
    return *s_the;
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
        CHttpRequest request;
        request.set_url(url);
        request.set_method(CHttpRequest::Method::GET);
        auto job = request.schedule();
        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();
        job->on_finish = [this, job, callback = move(callback)](bool success) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                dbg() << "HTTP job failed!";
                ASSERT_NOT_REACHED();
            }
            auto* response = job->response();
            ASSERT(response);
            callback(response->payload());
        };
        return;
    }

    dbg() << "Unimplemented protocol: " << url.protocol();
    ASSERT_NOT_REACHED();
}
