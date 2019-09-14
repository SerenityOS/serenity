#include <WindowServer/WSClipboard.h>

WSClipboard& WSClipboard::the()
{
    static WSClipboard* s_the;
    if (!s_the)
        s_the = new WSClipboard;
    return *s_the;
}

WSClipboard::WSClipboard()
{
}

WSClipboard::~WSClipboard()
{
}

const u8* WSClipboard::data() const
{
    if (!m_shared_buffer)
        return nullptr;
    return (const u8*)m_shared_buffer->data();
}

int WSClipboard::size() const
{
    if (!m_shared_buffer)
        return 0;
    return m_contents_size;
}

void WSClipboard::clear()
{
    m_shared_buffer = nullptr;
    m_contents_size = 0;
}

void WSClipboard::set_data(NonnullRefPtr<SharedBuffer>&& data, int contents_size, const String& data_type)
{
    dbg() << "WSClipboard::set_data <- [" << data_type << "] " << data->data() << " (" << contents_size << " bytes)";
    m_shared_buffer = move(data);
    m_contents_size = contents_size;
    m_data_type = data_type;

    if (on_content_change)
        on_content_change();
}
