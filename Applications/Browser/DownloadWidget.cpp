/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "DownloadWidget.h"
#include <AK/NumberFormat.h>
#include <AK/SharedBuffer.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/Window.h>
#include <LibProtocol/Client.h>
#include <LibWeb/ResourceLoader.h>
#include <math.h>

namespace Browser {

DownloadWidget::DownloadWidget(const URL& url)
    : m_url(url)
{
    {
        StringBuilder builder;
        builder.append(Core::StandardPaths::downloads_directory());
        builder.append('/');
        builder.append(m_url.basename());
        m_destination_path = builder.to_string();
    }

    m_elapsed_timer.start();
    m_download = Web::ResourceLoader::the().protocol_client().start_download(url.to_string());
    ASSERT(m_download);
    m_download->on_progress = [this](Optional<u32> total_size, u32 downloaded_size) {
        did_progress(total_size.value(), downloaded_size);
    };
    m_download->on_finish = [this](bool success, const ByteBuffer& payload, RefPtr<SharedBuffer> payload_storage, const HashMap<String, String>& response_headers) {
        did_finish(success, payload, payload_storage, response_headers);
    };

    set_fill_with_background_color(true);
    auto& layout = set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 4, 4, 4, 4 });

    auto& animation_container = add<GUI::Widget>();
    animation_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    animation_container.set_preferred_size(0, 32);
    auto& animation_layout = animation_container.set_layout<GUI::HorizontalBoxLayout>();
    auto& browser_icon_label = animation_container.add<GUI::Label>();
    browser_icon_label.set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-browser.png"));
    browser_icon_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    browser_icon_label.set_preferred_size(32, 32);
    animation_layout.add_spacer();
    auto& folder_icon_label = animation_container.add<GUI::Label>();
    folder_icon_label.set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-folder.png"));
    folder_icon_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    folder_icon_label.set_preferred_size(32, 32);

    auto& source_label = add<GUI::Label>(String::format("From: %s", url.to_string().characters()));
    source_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    source_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    source_label.set_preferred_size(0, 16);

    m_progress_bar = add<GUI::ProgressBar>();
    m_progress_bar->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_progress_bar->set_preferred_size(0, 20);

    m_progress_label = add<GUI::Label>();
    m_progress_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_progress_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_progress_label->set_preferred_size(0, 16);

    auto& destination_label = add<GUI::Label>(String::format("To: %s", m_destination_path.characters()));
    destination_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    destination_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    destination_label.set_preferred_size(0, 16);

    auto& button_container = add<GUI::Widget>();
    auto& button_container_layout = button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container_layout.add_spacer();
    m_cancel_button = button_container.add<GUI::Button>("Cancel");
    m_cancel_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_cancel_button->set_preferred_size(100, 22);
    m_cancel_button->on_click = [this] {
        bool success = m_download->stop();
        ASSERT(success);
        window()->close();
    };

    m_close_button = button_container.add<GUI::Button>("OK");
    m_close_button->set_enabled(false);
    m_close_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_close_button->set_preferred_size(100, 22);
    m_close_button->on_click = [this] {
        window()->close();
    };
}

DownloadWidget::~DownloadWidget()
{
}

void DownloadWidget::did_progress(Optional<u32> total_size, u32 downloaded_size)
{
    m_progress_bar->set_min(0);
    if (total_size.has_value())
        m_progress_bar->set_max(total_size.value());
    else
        m_progress_bar->set_max(0);
    m_progress_bar->set_value(downloaded_size);

    {
        StringBuilder builder;
        builder.append("Downloaded ");
        builder.append(human_readable_size(downloaded_size));
        builder.appendf(" in %d sec", m_elapsed_timer.elapsed() / 1000);
        m_progress_label->set_text(builder.to_string());
    }

    {
        StringBuilder builder;
        if (total_size.has_value()) {
            int percent = roundf(((float)downloaded_size / (float)total_size.value()) * 100);
            builder.appendf("%d%%", percent);
        } else {
            builder.append(human_readable_size(downloaded_size));
        }
        builder.append(" of ");
        builder.append(m_url.basename());
        window()->set_title(builder.to_string());
    }
}

void DownloadWidget::did_finish(bool success, const ByteBuffer& payload, RefPtr<SharedBuffer> payload_storage, const HashMap<String, String>& response_headers)
{
    (void)payload;
    (void)payload_storage;
    (void)response_headers;
    dbg() << "did_finish, success=" << success;
    m_cancel_button->set_enabled(false);
    m_close_button->set_enabled(true);

    if (!success) {
        GUI::MessageBox::show(String::format("Download failed for some reason"), "Download failed", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
        window()->close();
        return;
    }

    auto file_or_error = Core::File::open(m_destination_path, Core::IODevice::WriteOnly);
    if (file_or_error.is_error()) {
        GUI::MessageBox::show(String::format("Cannot open %s for writing", m_destination_path.characters()), "Download failed", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
        window()->close();
        return;
    }

    auto& file = *file_or_error.value();
    bool write_success = file.write(payload.data(), payload.size());
    ASSERT(write_success);
}

}
