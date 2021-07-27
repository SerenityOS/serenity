/*
 * Copyright (c) 2021, Łukasz Patron <priv.luk@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibCore/File.h>
#include <LibIPC/ServerConnection.h>
#include <stdio.h>
#include <unistd.h>

class ClipboardServerConnection final
    : public IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardClientEndpoint {
    C_OBJECT(ClipboardServerConnection);

private:
    ClipboardServerConnection()
        : IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, "/tmp/portal/clipboard")
    {
    }
    virtual void clipboard_data_changed(String const&) override {};
};

int main()
{
    Core::EventLoop loop;

    if (pledge("unix sendfd stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/tmp/portal/clipboard", "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto file = Core::File::standard_input();
    auto data = file->read_all();

    auto anon_buffer = Core::AnonymousBuffer::create_with_size(data.size());
    memcpy(anon_buffer.data<void>(), data.data(), data.size());

    auto conn = ClipboardServerConnection::construct();
    conn->async_set_clipboard_data(move(anon_buffer), "text/plain", {});

    return 0;
}
