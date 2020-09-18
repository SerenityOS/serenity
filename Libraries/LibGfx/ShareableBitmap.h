/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/RefPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>

namespace Gfx {

class ShareableBitmap {
public:
    ShareableBitmap() { }
    explicit ShareableBitmap(const Gfx::Bitmap&);

    bool is_valid() const { return m_bitmap; }

    i32 shbuf_id() const { return m_bitmap ? m_bitmap->shbuf_id() : -1; }

    const Bitmap* bitmap() const { return m_bitmap; }
    Bitmap* bitmap() { return m_bitmap; }

    IntSize size() const { return m_bitmap ? m_bitmap->size() : IntSize(); }
    IntRect rect() const { return m_bitmap ? m_bitmap->rect() : IntRect(); }

    int width() const { return size().width(); }
    int height() const { return size().height(); }

private:
    RefPtr<Bitmap> m_bitmap;
};

}

namespace IPC {

bool encode(Encoder&, const Gfx::ShareableBitmap&);
bool decode(Decoder&, Gfx::ShareableBitmap&);

}
