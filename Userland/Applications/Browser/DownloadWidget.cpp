/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DownloadWidget.h"
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
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

ErrorOr<NonnullRefPtr<DownloadWidget>> DownloadWidget::try_create(const URL& url)
{
    auto main_widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) DownloadWidget()));

    main_widget->m_url = url;

    {
        StringBuilder builder;
        builder.append(Core::StandardPaths::downloads_directory());
        builder.append('/');
        builder.append(url.basename());
        main_widget->m_destination_path = builder.to_deprecated_string();
    }

    auto close_on_finish = Config::read_bool("Browser"sv, "Preferences"sv, "CloseDownloadWidgetOnFinish"sv, false);

    main_widget->m_elapsed_timer.start();

    main_widget->m_download = Web::ResourceLoader::the().connector().start_request("GET", url);
    VERIFY(main_widget->m_download);

    main_widget->m_download->on_progress = [main_widget](Optional<u32> total_size, u32 downloaded_size) {
        main_widget->did_progress(total_size.value(), downloaded_size);
    };

    {
        auto file_or_error = Core::File::open(main_widget->m_destination_path, Core::File::OpenMode::Write);
        if (file_or_error.is_error()) {
            GUI::MessageBox::show(main_widget->window(), DeprecatedString::formatted("Cannot open {} for writing", main_widget->m_destination_path), "Download failed"sv, GUI::MessageBox::Type::Error);
            main_widget->window()->close();
            return file_or_error.release_error();
        }
        main_widget->m_output_file_stream = file_or_error.release_value();
    }

    main_widget->m_download->on_finish = [main_widget](bool success, auto) { main_widget->did_finish(success); };
    main_widget->m_download->stream_into(*main_widget->m_output_file_stream);

    main_widget->set_fill_with_background_color(true);
    TRY(main_widget->try_set_layout<GUI::VerticalBoxLayout>(4));

    auto animation_container = TRY(main_widget->try_add<GUI::Widget>());
    animation_container->set_fixed_height(32);
    TRY(animation_container->try_set_layout<GUI::HorizontalBoxLayout>());

    main_widget->m_browser_image = TRY(animation_container->try_add<GUI::ImageWidget>());
    main_widget->m_browser_image->load_from_file("/res/graphics/download-animation.gif"sv);
    TRY(animation_container->add_spacer());

    auto source_label = TRY(main_widget->try_add<GUI::Label>(DeprecatedString::formatted("From: {}", url)));
    source_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    source_label->set_fixed_height(16);

    main_widget->m_progressbar = TRY(main_widget->try_add<GUI::Progressbar>());
    main_widget->m_progressbar->set_fixed_height(20);

    main_widget->m_progress_label = TRY(main_widget->try_add<GUI::Label>());
    main_widget->m_progress_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    main_widget->m_progress_label->set_fixed_height(16);

    auto destination_label = TRY(main_widget->try_add<GUI::Label>(DeprecatedString::formatted("To: {}", main_widget->m_destination_path)));
    destination_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    destination_label->set_fixed_height(16);

    main_widget->m_close_on_finish_checkbox = TRY(main_widget->try_add<GUI::CheckBox>(TRY("Close when finished"_string)));
    main_widget->m_close_on_finish_checkbox->set_checked(close_on_finish);

    main_widget->m_close_on_finish_checkbox->on_checked = [&](bool checked) {
        Config::write_bool("Browser"sv, "Preferences"sv, "CloseDownloadWidgetOnFinish"sv, checked);
    };

    auto button_container = TRY(main_widget->try_add<GUI::Widget>());
    TRY(button_container->try_set_layout<GUI::HorizontalBoxLayout>());
    TRY(button_container->add_spacer());
    main_widget->m_cancel_button = TRY(button_container->try_add<GUI::Button>("Cancel"_short_string));
    main_widget->m_cancel_button->set_fixed_size(100, 22);
    main_widget->m_cancel_button->on_click = [main_widget](auto) {
        bool success = main_widget->m_download->stop();
        VERIFY(success);
        main_widget->window()->close();
    };

    main_widget->m_close_button = TRY(button_container->try_add<GUI::Button>("OK"_short_string));
    main_widget->m_close_button->set_enabled(false);
    main_widget->m_close_button->set_fixed_size(100, 22);
    main_widget->m_close_button->on_click = [main_widget](auto) {
        main_widget->window()->close();
    };

    return main_widget;
}

DownloadWidget::DownloadWidget()
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
        builder.append("Downloaded "sv);
        builder.append(human_readable_size(downloaded_size));
        builder.appendff(" in {} sec", m_elapsed_timer.elapsed_time().to_seconds());
        m_progress_label->set_text(builder.to_deprecated_string());
    }

    {
        StringBuilder builder;
        if (total_size.has_value()) {
            int percent = roundf(((float)downloaded_size / (float)total_size.value()) * 100);
            builder.appendff("{}%", percent);
        } else {
            builder.append(human_readable_size(downloaded_size));
        }
        builder.append(" of "sv);
        builder.append(m_url.basename());
        window()->set_title(builder.to_deprecated_string());
    }
}

void DownloadWidget::did_finish(bool success)
{
    dbgln("did_finish, success={}", success);

    m_browser_image->load_from_file("/res/graphics/download-finished.gif"sv);
    window()->set_title("Download finished!");
    m_close_button->set_enabled(true);
    m_cancel_button->set_text("Open in Folder"_string.release_value_but_fixme_should_propagate_errors());
    m_cancel_button->on_click = [this](auto) {
        Desktop::Launcher::open(URL::create_with_file_scheme(Core::StandardPaths::downloads_directory(), m_url.basename()));
        window()->close();
    };
    m_cancel_button->update();

    if (!success) {
        GUI::MessageBox::show(window(), DeprecatedString::formatted("Download failed for some reason"), "Download failed"sv, GUI::MessageBox::Type::Error);
        window()->close();
        return;
    }

    if (m_close_on_finish_checkbox->is_checked())
        window()->close();
}

}
