#include <ProtocolServer/Download.h>
#include <ProtocolServer/PSClientConnection.h>

// FIXME: What about rollover?
static i32 s_next_id = 1;

static HashMap<i32, RefPtr<Download>>& all_downloads()
{
    static HashMap<i32, RefPtr<Download>> map;
    return map;
}

Download* Download::find_by_id(i32 id)
{
    return all_downloads().get(id).value_or(nullptr);
}

Download::Download(PSClientConnection& client)
    : m_id(s_next_id++)
    , m_client(client.make_weak_ptr())
{
    all_downloads().set(m_id, this);
}

Download::~Download()
{
}

void Download::stop()
{
    all_downloads().remove(m_id);
}

void Download::did_finish(bool success)
{
    if (!m_client) {
        dbg() << "Download::did_finish() after the client already disconnected.";
        return;
    }
    m_client->did_finish_download({}, *this, success);
    all_downloads().remove(m_id);
}

void Download::did_progress(size_t total_size, size_t downloaded_size)
{
    if (!m_client) {
        // FIXME: We should also abort the download in this situation, I guess!
        dbg() << "Download::did_progress() after the client already disconnected.";
        return;
    }
    m_total_size = total_size;
    m_downloaded_size = downloaded_size;
    m_client->did_progress_download({}, *this);
}

