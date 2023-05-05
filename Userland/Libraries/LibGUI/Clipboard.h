/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
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

        virtual void clipboard_content_did_change(DeprecatedString const& mime_type) = 0;
    };

    struct DataAndType {
        ByteBuffer data;
        DeprecatedString mime_type;
        HashMap<DeprecatedString, DeprecatedString> metadata;

        RefPtr<Gfx::Bitmap> as_bitmap() const;
    };

    static ErrorOr<void> initialize(Badge<Application>);
    static Clipboard& the();

    DataAndType fetch_data_and_type() const;
    DeprecatedString fetch_mime_type() const { return fetch_data_and_type().mime_type; }

    void set_data(ReadonlyBytes data, DeprecatedString const& mime_type = "text/plain", HashMap<DeprecatedString, DeprecatedString> const& metadata = {});
    void set_plain_text(StringView text) { set_data(text.bytes()); }
    void set_bitmap(Gfx::Bitmap const&, HashMap<DeprecatedString, DeprecatedString> const& additional_metadata = {});
    void clear();

    void clipboard_data_changed(Badge<ConnectionToClipboardServer>, DeprecatedString const& mime_type);

    void register_client(Badge<ClipboardClient>, ClipboardClient& client) { m_clients.set(&client); }
    void unregister_client(Badge<ClipboardClient>, ClipboardClient& client) { m_clients.remove(&client); }

    Function<void(DeprecatedString const& mime_type)> on_change;

private:
    Clipboard() = default;

    HashTable<ClipboardClient*> m_clients;
};

}
