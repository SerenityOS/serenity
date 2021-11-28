/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>

namespace Gfx {

class ShareableBitmap {
public:
    ShareableBitmap() { }

    enum Tag { ConstructWithKnownGoodBitmap };
    ShareableBitmap(NonnullRefPtr<Gfx::Bitmap>, Tag);

    bool is_valid() const { return m_bitmap; }

    const Bitmap* bitmap() const { return m_bitmap; }
    Bitmap* bitmap() { return m_bitmap; }

private:
    friend class Bitmap;

    RefPtr<Bitmap> m_bitmap;
};

}

namespace IPC {

bool encode(Encoder&, const Gfx::ShareableBitmap&);
ErrorOr<void> decode(Decoder&, Gfx::ShareableBitmap&);

}
