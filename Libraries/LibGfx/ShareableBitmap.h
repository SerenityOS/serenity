#pragma once

#include <AK/RefPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>

namespace Gfx {

class ShareableBitmap {
public:
    ShareableBitmap() {}
    explicit ShareableBitmap(const Gfx::Bitmap&);

    bool is_valid() const { return m_bitmap; }

    i32 shbuf_id() const { return m_bitmap ? m_bitmap->shbuf_id() : -1; }

    const Bitmap* bitmap() const { return m_bitmap; }
    Bitmap* bitmap() { return m_bitmap; }

    Size size() const { return m_bitmap ? m_bitmap->size() : Size(); }
    Rect rect() const { return m_bitmap ? m_bitmap->rect() : Rect(); }

    int width() const { return size().width(); }
    int height() const { return size().height(); }

private:
    RefPtr<Bitmap> m_bitmap;
};

}

namespace IPC {
bool decode(Decoder&, Gfx::ShareableBitmap&);
}
