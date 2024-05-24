/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DownloadWidget.h"
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
#include <Applications/BrowserSettings/Defaults.h>
#include <LibCore/Proxy.h>
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
#include <LibWeb/Loader/ResourceLoader.h>

#include <LibConfig/Client.h>

namespace Browser {

DownloadWidget::DownloadWidget(const URL::URL& url)
    : m_url(url)
{
    {
        StringBuilder builder;
        builder.append(Core::StandardPaths::downloads_directory());
        builder.append('/');
        builder.append(m_url.basename());
        m_destination_path = builder.to_byte_string();
    }

    auto close_on_finish = Config::read_bool("Browser"sv, "Preferences"sv, "CloseDownloadWidgetOnFinish"sv, Browser::default_close_download_widget_on_finish);

    m_elapsed_timer.start();
    m_download = Web::ResourceLoader::the().connector().start_request("GET", url);
    VERIFY(m_download);

    m_download->on_progress = [this](Optional<u64> total_size, u64 downloaded_size) {
        did_progress(move(total_size), downloaded_size);
    };

    {
        auto file_or_error = Core::File::open(m_destination_path, Core::File::OpenMode::Write);
        if (file_or_error.is_error()) {
            GUI::MessageBox::show(window(), ByteString::formatted("Cannot open {} for writing", m_destination_path), "Download failed"sv, GUI::MessageBox::Type::Error);
            window()->close();
            return;
        }
        m_output_file_stream = file_or_error.release_value();
    }

    auto on_data_received = [this](auto data) {
        m_output_file_stream->write_until_depleted(data).release_value_but_fixme_should_propagate_errors();
    };

    auto on_finished = [this](bool success, auto) {
        did_finish(success);
    };

    m_download->set_unbuffered_request_callbacks({}, move(on_data_received), move(on_finished));

    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>(4);

    auto& animation_container = add<GUI::Widget>();
    animation_container.set_fixed_height(32);
    animation_container.set_layout<GUI::HorizontalBoxLayout>();

    m_browser_image = animation_container.add<GUI::ImageWidget>();
    m_browser_image->load_from_file("/res/graphics/download-animation.gif"sv);
    animation_container.add_spacer();

    auto& source_label = add<GUI::Label>(String::formatted("File: {}", m_url.basename()).release_value_but_fixme_should_propagate_errors());
    source_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    source_label.set_fixed_height(16);
    source_label.set_text_wrapping(Gfx::TextWrapping::DontWrap);

    m_progressbar = add<GUI::Progressbar>();
    m_progressbar->set_fixed_height(20);
    m_progressbar->set_min(0);
    m_progressbar->set_max(100);

    m_progress_label = add<GUI::Label>();
    m_progress_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_progress_label->set_fixed_height(16);
    m_progress_label->set_text_wrapping(Gfx::TextWrapping::DontWrap);

    auto destination_label_path = LexicalPath(m_destination_path).dirname().to_byte_string();

    auto& destination_label = add<GUI::Label>(String::formatted("To: {}", destination_label_path).release_value_but_fixme_should_propagate_errors());
    destination_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    destination_label.set_fixed_height(16);
    destination_label.set_text_wrapping(Gfx::TextWrapping::DontWrap);

    m_close_on_finish_checkbox = add<GUI::CheckBox>("Close when finished"_string);
    m_close_on_finish_checkbox->set_checked(close_on_finish);

    m_close_on_finish_checkbox->on_checked = [&](bool checked) {
        Config::write_bool("Browser"sv, "Preferences"sv, "CloseDownloadWidgetOnFinish"sv, checked);
    };

    auto& button_container = add<GUI::Widget>();
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.add_spacer();
    m_cancel_button = button_container.add<GUI::Button>("Cancel"_string);
    m_cancel_button->set_fixed_size(100, 22);
    m_cancel_button->on_click = [this](auto) {
        bool success = m_download->stop();
        VERIFY(success);
        window()->close();
    };

    m_close_button = button_container.add<GUI::Button>("OK"_string);
    m_close_button->set_enabled(false);
    m_close_button->set_fixed_size(100, 22);
    m_close_button->on_click = [this](auto) {
        window()->close();
    };
}

void DownloadWidget::did_progress(Optional<u64> total_size, u64 downloaded_size)
{
    int percent = 0;
    if (total_size.has_value()) {
        percent = downloaded_size * 100 / total_size.value();
        window()->set_progress(percent);
        m_progressbar->set_value(percent);
    }

    {
        StringBuilder builder;
        builder.append("Downloaded "sv);
        builder.append(human_readable_size(downloaded_size));
        builder.appendff(" in {} sec", m_elapsed_timer.elapsed_time().to_seconds());
        m_progress_label->set_text(builder.to_string().release_value_but_fixme_should_propagate_errors());
    }

    {
        StringBuilder builder;
        if (total_size.has_value()) {
            builder.appendff("{}%", percent);
        } else {
            builder.append(human_readable_size(downloaded_size));
        }
        builder.append(" of "sv);
        builder.append(m_url.basename());
        window()->set_title(builder.to_byte_string());
    }
}

void DownloadWidget::did_finish(bool success)
{
    dbgln("did_finish, success={}", success);

    m_browser_image->load_from_file("/res/graphics/download-finished.gif"sv);
    window()->set_title("Download finished!");
    m_close_button->set_enabled(true);
    m_cancel_button->set_text("Open in Folder"_string);
    m_cancel_button->on_click = [this](auto) {
        Desktop::Launcher::open(URL::create_with_file_scheme(Core::StandardPaths::downloads_directory(), m_url.basename()));
        window()->close();
    };
    m_cancel_button->update();

    if (!success) {
        GUI::MessageBox::show(window(), ByteString::formatted("Download failed for some reason"), "Download failed"sv, GUI::MessageBox::Type::Error);
        window()->close();
        return;
    }

    if (m_close_on_finish_checkbox->is_checked())
        window()->close();
}

}
