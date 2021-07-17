/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DownloadWindow.h"
#include "DownloadWidget.h"
#include <AK/HashMap.h>

namespace FileSystemAccessServer {

static HashMap<u32, NonnullRefPtr<DownloadWindow>> s_windows;

DownloadWindow::DownloadWindow(i32 client_id, URL const& url, String const& destination_path)
{
    m_id = client_id;
    s_windows.set(m_id, *this);

    resize(300, 170);
    set_title(String::formatted("0% of {}", destination_path));
    set_resizable(false);

    set_main_widget<DownloadWidget>(url, destination_path);

    on_close = [this] {
        s_windows.remove(m_id);
    };
}

DownloadWindow::~DownloadWindow()
{
}

}
