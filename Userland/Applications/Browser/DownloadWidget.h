/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Widget.h>
#include <LibURL/URL.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Browser {

class DownloadWidget final : public GUI::Widget {
    C_OBJECT(DownloadWidget);

public:
    virtual ~DownloadWidget() override = default;

private:
    explicit DownloadWidget(const URL::URL&);

    void did_progress(Optional<u64> total_size, u64 downloaded_size);
    void did_finish(bool success);

    URL::URL m_url;
    ByteString m_destination_path;
    RefPtr<Web::ResourceLoaderConnectorRequest> m_download;
    RefPtr<GUI::Progressbar> m_progressbar;
    RefPtr<GUI::Label> m_progress_label;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_close_button;
    RefPtr<GUI::CheckBox> m_close_on_finish_checkbox;
    RefPtr<GUI::ImageWidget> m_browser_image;
    OwnPtr<Core::File> m_output_file_stream;
    Core::ElapsedTimer m_elapsed_timer;
};

}
