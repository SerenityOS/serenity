/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

class Clipboard {
public:
    static Clipboard& the();

    ByteBuffer data() const { return data_and_type().data; }
    String mime_type() const { return data_and_type().mime_type; }
    void set_data(ReadonlyBytes, const String& mime_type = "text/plain", const HashMap<String, String>& metadata = {});

    void set_plain_text(const String& text)
    {
        set_data(text.bytes());
    }

    void set_bitmap(const Gfx::Bitmap&);
    RefPtr<Gfx::Bitmap> bitmap() const;

    struct DataAndType {
        ByteBuffer data;
        String mime_type;
        HashMap<String, String> metadata;
    };

    DataAndType data_and_type() const;

    Function<void(const String& mime_type)> on_change;

    static void initialize(Badge<Application>);

private:
    Clipboard();
};

}
