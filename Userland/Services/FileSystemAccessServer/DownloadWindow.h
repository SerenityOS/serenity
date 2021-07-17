/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Window.h>

namespace FileSystemAccessServer {
class DownloadWindow final : public GUI::Window {
    C_OBJECT(DownloadWindow);

public:
    virtual ~DownloadWindow() override;

private:
    DownloadWindow(i32 client_id, URL const& url, String const& destination_path);

    i32 m_id;
};

}
