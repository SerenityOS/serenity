/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/FileStream.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Widget.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

namespace FileSystemAccessServer {

class DownloadWidget final : public GUI::Widget {
    C_OBJECT(DownloadWidget);

public:
    virtual ~DownloadWidget() override;

private:
    DownloadWidget(URL const&, String const& destination_path);

    void did_progress(Optional<u32> total_size, u32 downloaded_size);
    void did_finish(bool success);

    URL m_url;
    String m_destination_path;
    NonnullRefPtr<Protocol::RequestClient> m_protocol_client;
    RefPtr<Protocol::Request> m_download;
    RefPtr<GUI::Progressbar> m_progressbar;
    RefPtr<GUI::Label> m_progress_label;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_close_button;
    RefPtr<GUI::CheckBox> m_close_on_finish_checkbox;
    RefPtr<GUI::ImageWidget> m_browser_image;
    OwnPtr<Core::OutputFileStream> m_output_file_stream;
    Core::ElapsedTimer m_elapsed_timer;
};

}
