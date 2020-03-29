#include <AK/SharedBuffer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibIPC/Decoder.h>

namespace Gfx {

ShareableBitmap::ShareableBitmap(const Bitmap& bitmap)
    : m_bitmap(bitmap.to_bitmap_backed_by_shared_buffer())
{
}

}

namespace IPC {

bool decode(Decoder& decoder, Gfx::ShareableBitmap& shareable_bitmap)
{
    i32 shbuf_id = 0;
    Gfx::Size size;
    if (!decoder.decode(shbuf_id))
        return false;
    if (!decoder.decode(size))
        return false;

    if (shbuf_id == -1)
        return true;

    dbg() << "Decoding a ShareableBitmap with shbuf_id=" << shbuf_id << ", size=" << size;

    auto shared_buffer = SharedBuffer::create_from_shbuf_id(shbuf_id);
    if (!shared_buffer)
        return false;

    auto bitmap = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, shared_buffer.release_nonnull(), size);
    shareable_bitmap = bitmap->to_shareable_bitmap();
    return true;
}

}
