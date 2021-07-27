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

    if (pledge("unix recvfd stdio", nullptr) < 0) {
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

    auto conn = ClipboardServerConnection::construct();
    auto buf = conn->get_clipboard_data().data();

    auto file = Core::File::standard_output();
    file->write(buf.data<u8>(), buf.size());

    return 0;
}
