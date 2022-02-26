/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
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

class ConnectionToClipboardServer;

class Clipboard {
public:
    class ClipboardClient {
    public:
        ClipboardClient();
        virtual ~ClipboardClient();

        virtual void clipboard_content_did_change(String const& mime_type) = 0;
    };

    struct DataAndType {
        ByteBuffer data;
        String mime_type;
        HashMap<String, String> metadata;

        RefPtr<Gfx::Bitmap> as_bitmap() const;
    };

    static void initialize(Badge<Application>);
    static Clipboard& the();

    DataAndType fetch_data_and_type() const;
    String fetch_mime_type() const { return fetch_data_and_type().mime_type; }

    void set_data(ReadonlyBytes data, String const& mime_type = "text/plain", HashMap<String, String> const& metadata = {});
    void set_plain_text(String const& text) { set_data(text.bytes()); }
    void set_bitmap(Gfx::Bitmap const&);
    void clear();

    void clipboard_data_changed(Badge<ConnectionToClipboardServer>, String const& mime_type);

    void register_client(Badge<ClipboardClient>, ClipboardClient& client) { m_clients.set(&client); }
    void unregister_client(Badge<ClipboardClient>, ClipboardClient& client) { m_clients.remove(&client); }

    Function<void(String const& mime_type)> on_change;

private:
    Clipboard() = default;

    HashTable<ClipboardClient*> m_clients;
};

}
