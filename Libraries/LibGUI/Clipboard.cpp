/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Badge.h>
#include <AK/SharedBuffer.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibGUI/Clipboard.h>
#include <LibIPC/ServerConnection.h>

namespace GUI {

class ClipboardServerConnection : public IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardClientEndpoint {
    C_OBJECT(ClipboardServerConnection);

public:
    virtual void handshake() override
    {
        auto response = send_sync<Messages::ClipboardServer::Greet>();
        set_my_client_id(response->client_id());
    }

private:
    ClipboardServerConnection()
        : IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, "/tmp/portal/clipboard")
    {
    }
    virtual void handle(const Messages::ClipboardClient::ClipboardDataChanged&) override;
};

Clipboard& Clipboard::the()
{
    static Clipboard* s_the;
    if (!s_the)
        s_the = new Clipboard;
    return *s_the;
}

ClipboardServerConnection* s_connection;

static ClipboardServerConnection& connection()
{
    return *s_connection;
}

void Clipboard::initialize(Badge<Application>)
{
    s_connection = &ClipboardServerConnection::construct().leak_ref();
}

Clipboard::Clipboard()
{
}

Clipboard::DataAndType Clipboard::data_and_type() const
{
    auto response = connection().send_sync<Messages::ClipboardServer::GetClipboardData>();
    if (response->shbuf_id() < 0)
        return {};
    auto shared_buffer = SharedBuffer::create_from_shbuf_id(response->shbuf_id());
    if (!shared_buffer) {
        dbgprintf("GUI::Clipboard::data() failed to attach to the shared buffer\n");
        return {};
    }
    if (response->data_size() > shared_buffer->size()) {
        dbgprintf("GUI::Clipboard::data() clipping contents size is greater than shared buffer size\n");
        return {};
    }
    auto data = ByteBuffer::copy(shared_buffer->data<void>(), response->data_size());
    auto type = response->mime_type();
    auto metadata = response->metadata().entries();
    return { data, type, metadata };
}

void Clipboard::set_data(ReadonlyBytes data, const String& type, const HashMap<String, String>& metadata)
{
    auto shared_buffer = SharedBuffer::create_with_size(data.size());
    if (!shared_buffer) {
        dbgprintf("GUI::Clipboard::set_data() failed to create a shared buffer\n");
        return;
    }
    if (!data.is_empty())
        memcpy(shared_buffer->data<void>(), data.data(), data.size());
    shared_buffer->seal();
    shared_buffer->share_with(connection().server_pid());

    connection().send_sync<Messages::ClipboardServer::SetClipboardData>(shared_buffer->shbuf_id(), data.size(), type, metadata);
}

void ClipboardServerConnection::handle(const Messages::ClipboardClient::ClipboardDataChanged& message)
{
    auto& clipboard = Clipboard::the();
    if (clipboard.on_change)
        clipboard.on_change(message.mime_type());
}

RefPtr<Gfx::Bitmap> Clipboard::bitmap() const
{
    auto clipping = data_and_type();

    if (clipping.mime_type != "image/x-serenityos")
        return nullptr;

    auto width = clipping.metadata.get("width").value_or("0").to_uint();
    if (!width.has_value() || width.value() == 0)
        return nullptr;

    auto height = clipping.metadata.get("height").value_or("0").to_uint();
    if (!height.has_value() || height.value() == 0)
        return nullptr;

    auto pitch = clipping.metadata.get("pitch").value_or("0").to_uint();
    if (!pitch.has_value() || pitch.value() == 0)
        return nullptr;

    auto format = clipping.metadata.get("format").value_or("0").to_uint();
    if (!format.has_value() || format.value() == 0)
        return nullptr;

    auto clipping_bitmap = Gfx::Bitmap::create_wrapper((Gfx::BitmapFormat)format.value(), { (int)width.value(), (int)height.value() }, pitch.value(), clipping.data.data());
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, { (int)width.value(), (int)height.value() });

    for (int y = 0; y < clipping_bitmap->height(); ++y) {
        for (int x = 0; x < clipping_bitmap->width(); ++x) {
            auto pixel = clipping_bitmap->get_pixel(x, y);
            bitmap->set_pixel(x, y, pixel);
        }
    }

    return bitmap;
}

void Clipboard::set_bitmap(const Gfx::Bitmap& bitmap)
{
    HashMap<String, String> metadata;
    metadata.set("width", String::number(bitmap.width()));
    metadata.set("height", String::number(bitmap.height()));
    metadata.set("format", String::number((int)bitmap.format()));
    metadata.set("pitch", String::number(bitmap.pitch()));
    metadata.set("bpp", String::number(bitmap.bpp()));
    set_data({ bitmap.scanline(0), bitmap.size_in_bytes() }, "image/x-serenityos", metadata);
}

}
