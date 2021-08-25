/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DownloadWidget.h"
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Window.h>
#include <LibProtocol/RequestClient.h>
#include <LibWeb/Loader/ResourceLoader.h>

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

    auto close_on_finish = Config::read_bool("Browser", "Preferences", "CloseDownloadWidgetOnFinish", false);

    m_elapsed_timer.start();
    m_download = Web::ResourceLoader::the().protocol_client().start_request("GET", url);
    VERIFY(m_download);
    m_download->on_progress = [this](Optional<u32> total_size, u32 downloaded_size) {
        did_progress(total_size.value(), downloaded_size);
    };

    {
        auto file_or_error = Core::File::open(m_destination_path, Core::OpenMode::WriteOnly);
        if (file_or_error.is_error()) {
            GUI::MessageBox::show(window(), String::formatted("Cannot open {} for writing", m_destination_path), "Download failed", GUI::MessageBox::Type::Error);
            window()->close();
            return;
        }
        m_output_file_stream = make<Core::OutputFileStream>(*file_or_error.value());
    }

    m_download->on_finish = [this](bool success, auto) { did_finish(success); };
    m_download->stream_into(*m_output_file_stream);

    set_fill_with_background_color(true);
    auto& layout = set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    auto& animation_container = add<GUI::Widget>();
    animation_container.set_fixed_height(32);
    auto& animation_layout = animation_container.set_layout<GUI::HorizontalBoxLayout>();

    m_browser_image = animation_container.add<GUI::ImageWidget>();
    m_browser_image->load_from_file("/res/graphics/download-animation.gif");
    animation_layout.add_spacer();

    auto& source_label = add<GUI::Label>(String::formatted("From: {}", url));
    source_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    source_label.set_fixed_height(16);

    m_progressbar = add<GUI::Progressbar>();
    m_progressbar->set_fixed_height(20);

    m_progress_label = add<GUI::Label>();
    m_progress_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_progress_label->set_fixed_height(16);

    auto& destination_label = add<GUI::Label>(String::formatted("To: {}", m_destination_path));
    destination_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    destination_label.set_fixed_height(16);

    m_close_on_finish_checkbox = add<GUI::CheckBox>("Close when finished");
    m_close_on_finish_checkbox->set_checked(close_on_finish);

    m_close_on_finish_checkbox->on_checked = [&](bool checked) {
        Config::write_bool("Browser", "Preferences", "CloseDownloadWidgetOnFinish", checked);
    };

    auto& button_container = add<GUI::Widget>();
    auto& button_container_layout = button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container_layout.add_spacer();
    m_cancel_button = button_container.add<GUI::Button>("Cancel");
    m_cancel_button->set_fixed_size(100, 22);
    m_cancel_button->on_click = [this](auto) {
        bool success = m_download->stop();
        VERIFY(success);
        window()->close();
    };

    m_close_button = button_container.add<GUI::Button>("OK");
    m_close_button->set_enabled(false);
    m_close_button->set_fixed_size(100, 22);
    m_close_button->on_click = [this](auto) {
        window()->close();
    };
}

DownloadWidget::~DownloadWidget()
{
}

void DownloadWidget::did_progress(Optional<u32> total_size, u32 downloaded_size)
{
    m_progressbar->set_min(0);
    if (total_size.has_value()) {
        int percent = roundf(((float)downloaded_size / (float)total_size.value()) * 100.0f);
        window()->set_progress(percent);
        m_progressbar->set_max(total_size.value());
    } else {
        m_progressbar->set_max(0);
    }
    m_progressbar->set_value(downloaded_size);

    {
        StringBuilder builder;
        builder.append("Downloaded ");
        builder.append(human_readable_size(downloaded_size));
        builder.appendff(" in {} sec", m_elapsed_timer.elapsed() / 1000);
        m_progress_label->set_text(builder.to_string());
    }

    {
        StringBuilder builder;
        if (total_size.has_value()) {
            int percent = roundf(((float)downloaded_size / (float)total_size.value()) * 100);
            builder.appendff("{}%", percent);
        } else {
            builder.append(human_readable_size(downloaded_size));
        }
        builder.append(" of ");
        builder.append(m_url.basename());
        window()->set_title(builder.to_string());
    }
}

void DownloadWidget::did_finish(bool success)
{
    dbgln("did_finish, success={}", success);

    m_browser_image->load_from_file("/res/graphics/download-finished.gif");
    window()->set_title("Download finished!");
    m_close_button->set_enabled(true);
    m_cancel_button->set_text("Open in Folder");
    m_cancel_button->on_click = [this](auto) {
        Desktop::Launcher::open(URL::create_with_file_protocol(Core::StandardPaths::downloads_directory(), m_url.basename()));
        window()->close();
    };
    m_cancel_button->update();

    if (!success) {
        GUI::MessageBox::show(window(), String::formatted("Download failed for some reason"), "Download failed", GUI::MessageBox::Type::Error);
        window()->close();
        return;
    }

    if (m_close_on_finish_checkbox->is_checked())
        window()->close();
}

}
