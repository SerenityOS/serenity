/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>

namespace GUI {

class ClipboardServerConnection;

class Clipboard {
public:
    class ClipboardClient {
    public:
        ClipboardClient() = default;
        virtual ~ClipboardClient() = default;

        virtual void clipboard_content_did_change(String const& mime_type) = 0;
    };

    struct DataAndType {
        ByteBuffer data;
        String mime_type;
        HashMap<String, String> metadata;

        RefPtr<Gfx::Bitmap> as_bitmap() const;
    };

    static void initialize(Badge<Application>) { }
    static Clipboard& the();

    DataAndType fetch_data_and_type() const { return {}; }
    String fetch_mime_type() const { return ""; }

    void set_data(ReadonlyBytes, String const&, HashMap<String, String> const&) { }
    void set_plain_text(String const&) { }
    void set_bitmap(Gfx::Bitmap const&) { }
    void clear() { }

private:
    Clipboard() = default;
};

}
