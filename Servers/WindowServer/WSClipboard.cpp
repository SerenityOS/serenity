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

const byte* WSClipboard::data() const
{
    if (!m_shared_buffer)
        return nullptr;
    return (const byte*)m_shared_buffer->data();
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

void WSClipboard::set_data(Retained<SharedBuffer>&& data, int contents_size)
{
    dbgprintf("WSClipboard::set_data <- %p (%u bytes)\n", data->data(), contents_size);
    m_shared_buffer = move(data);
    m_contents_size = contents_size;
}
