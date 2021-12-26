#include <LibGUI/GClipboard.h>
#include <LibGUI/GEventLoop.h>
#include <WindowServer/WSAPITypes.h>
#include <LibC/SharedBuffer.h>

GClipboard& GClipboard::the()
{
    static GClipboard* s_the;
    if (!s_the)
        s_the = new GClipboard;
    return *s_the;
}

GClipboard::GClipboard()
{
}

String GClipboard::data() const
{
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetClipboardContents;
    auto response = GEventLoop::current().sync_request(request, WSAPI_ServerMessage::Type::DidGetClipboardContents);
    if (response.clipboard.shared_buffer_id < 0)
        return { };
    auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(response.clipboard.shared_buffer_id);
    if (!shared_buffer) {
        dbgprintf("GClipboard::data() failed to attach to the shared buffer\n");
        return { };
    }
    if (response.clipboard.contents_size > shared_buffer->size()) {
        dbgprintf("GClipboard::data() clipping contents size is greater than shared buffer size\n");
        return { };
    }
    return String((const char*)shared_buffer->data(), response.clipboard.contents_size);
}

void GClipboard::set_data(const String& data)
{
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetClipboardContents;
    auto shared_buffer = SharedBuffer::create(GEventLoop::current().server_pid(), data.length() + 1);
    if (!shared_buffer) {
        dbgprintf("GClipboard::set_data() failed to create a shared buffer\n");
        return;
    }
    if (!data.is_empty())
        memcpy(shared_buffer->data(), data.characters(), data.length() + 1);
    else
        ((byte*)shared_buffer->data())[0] = '\0';
    shared_buffer->seal();
    request.clipboard.shared_buffer_id = shared_buffer->shared_buffer_id();
    request.clipboard.contents_size = data.length();
    auto response = GEventLoop::current().sync_request(request, WSAPI_ServerMessage::Type::DidSetClipboardContents);
    ASSERT(response.clipboard.shared_buffer_id == shared_buffer->shared_buffer_id());
}
