/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PropertiesWindow.h"
#include <AK/GenericShorthands.h>
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <Applications/FileManager/DirectoryView.h>
#include <Applications/FileManager/PropertiesWindowArchiveTabGML.h>
#include <Applications/FileManager/PropertiesWindowAudioTabGML.h>
#include <Applications/FileManager/PropertiesWindowFontTabGML.h>
#include <Applications/FileManager/PropertiesWindowGeneralTabGML.h>
#include <Applications/FileManager/PropertiesWindowImageTabGML.h>
#include <Applications/FileManager/PropertiesWindowPDFTabGML.h>
#include <LibArchive/Zip.h>
#include <LibAudio/Loader.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/IconView.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/TabWidget.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/Font/FontWeight.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/Typeface.h>
#include <LibGfx/Font/WOFF/Font.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/Tags.h>
#include <LibGfx/ImageFormats/ExifGPS.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>
#include <LibMaps/MapWidget.h>
#include <LibPDF/Document.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

ErrorOr<NonnullRefPtr<PropertiesWindow>> PropertiesWindow::try_create(ByteString const& path, bool disable_rename, Window* parent)
{
    auto window = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PropertiesWindow(path, parent)));
    window->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/properties.png"sv)));
    TRY(window->create_widgets(disable_rename));
    return window;
}

PropertiesWindow::PropertiesWindow(ByteString const& path, Window* parent_window)
    : Window(parent_window)
{
    auto lexical_path = LexicalPath(path);

    m_name = lexical_path.basename();
    m_path = lexical_path.string();
    m_parent_path = lexical_path.dirname();

    set_rect({ 0, 0, 360, 420 });
    set_resizable(false);
}

ErrorOr<void> PropertiesWindow::create_widgets(bool disable_rename)
{
    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_layout<GUI::VerticalBoxLayout>(4, 6);
    main_widget->set_fill_with_background_color(true);

    auto& tab_widget = main_widget->add<GUI::TabWidget>();
    TRY(create_general_tab(tab_widget, disable_rename));
    TRY(create_file_type_specific_tabs(tab_widget));

    auto& button_widget = main_widget->add<GUI::Widget>();
    button_widget.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 5);
    button_widget.set_fixed_height(22);

    button_widget.add_spacer();

    auto& ok_button = make_button("OK"_string, button_widget);
    ok_button.on_click = [this](auto) {
        if (apply_changes())
            close();
    };
    auto& cancel_button = make_button("Cancel"_string, button_widget);
    cancel_button.on_click = [this](auto) {
        close();
    };

    m_apply_button = make_button("Apply"_string, button_widget);
    m_apply_button->on_click = [this](auto) { apply_changes(); };
    m_apply_button->set_enabled(false);

    if (S_ISDIR(m_old_mode)) {
        m_directory_statistics_calculator = make_ref_counted<DirectoryStatisticsCalculator>(m_path);
        m_directory_statistics_calculator->on_update = [this, origin_event_loop = &Core::EventLoop::current()](off_t total_size_in_bytes, size_t file_count, size_t directory_count) {
            origin_event_loop->deferred_invoke([=, weak_this = make_weak_ptr<PropertiesWindow>()] {
                if (auto strong_this = weak_this.strong_ref())
                    strong_this->m_size_label->set_text(MUST(String::formatted("{}\n{} files, {} subdirectories", human_readable_size_long(total_size_in_bytes, UseThousandsSeparator::Yes), file_count, directory_count)));
            });
        };
        m_directory_statistics_calculator->start();
    }

    m_on_escape = GUI::Action::create("Close properties", { Key_Escape }, [this](GUI::Action&) {
        if (!m_apply_button->is_enabled())
            close();
    });

    update();
    return {};
}

ErrorOr<void> PropertiesWindow::create_general_tab(GUI::TabWidget& tab_widget, bool disable_rename)
{
    auto& general_tab = tab_widget.add_tab<GUI::Widget>("General"_string);
    TRY(general_tab.load_from_gml(properties_window_general_tab_gml));

    m_icon = general_tab.find_descendant_of_type_named<GUI::ImageWidget>("icon");

    m_name_box = general_tab.find_descendant_of_type_named<GUI::TextBox>("name");
    m_name_box->set_text(m_name);
    m_name_box->set_mode(disable_rename ? GUI::TextBox::Mode::DisplayOnly : GUI::TextBox::Mode::Editable);
    m_name_box->on_change = [&]() {
        m_name_dirty = m_name != m_name_box->text();
        m_apply_button->set_enabled(m_name_dirty || m_permissions_dirty);
    };

    auto* location = general_tab.find_descendant_of_type_named<GUI::LinkLabel>("location");
    location->set_text(TRY(String::from_byte_string(m_path)));
    location->on_click = [this] {
        Desktop::Launcher::open(URL::create_with_file_scheme(m_parent_path, m_name));
    };

    auto st = TRY(Core::System::lstat(m_path));

    ByteString owner_name;
    ByteString group_name;

    if (auto* pw = getpwuid(st.st_uid)) {
        owner_name = pw->pw_name;
    } else {
        owner_name = "n/a";
    }

    if (auto* gr = getgrgid(st.st_gid)) {
        group_name = gr->gr_name;
    } else {
        group_name = "n/a";
    }

    m_mode = st.st_mode;
    m_old_mode = st.st_mode;

    auto* type = general_tab.find_descendant_of_type_named<GUI::Label>("type");
    type->set_text(TRY(String::from_utf8(get_description(m_mode))));

    if (S_ISLNK(m_mode)) {
        auto link_destination_or_error = FileSystem::read_link(m_path);
        if (link_destination_or_error.is_error()) {
            perror("readlink");
        } else {
            auto link_destination = link_destination_or_error.release_value();
            auto* link_location = general_tab.find_descendant_of_type_named<GUI::LinkLabel>("link_location");
            // FIXME: How do we safely display some text that might not be utf8?
            auto link_destination_string = TRY(String::from_byte_string(link_destination));
            link_location->set_text(link_destination_string);
            link_location->on_click = [link_destination] {
                auto link_directory = LexicalPath(link_destination);
                Desktop::Launcher::open(URL::create_with_file_scheme(link_directory.dirname(), link_directory.basename()));
            };
        }
    } else {
        auto* link_location_widget = general_tab.find_descendant_of_type_named<GUI::Widget>("link_location_widget");
        general_tab.remove_child(*link_location_widget);
    }

    m_size_label = general_tab.find_descendant_of_type_named<GUI::Label>("size");
    m_size_label->set_text(S_ISDIR(st.st_mode)
            ? "Calculating..."_string
            : human_readable_size_long(st.st_size, UseThousandsSeparator::Yes));

    auto* owner = general_tab.find_descendant_of_type_named<GUI::Label>("owner");
    owner->set_text(String::formatted("{} ({})", owner_name, st.st_uid).release_value_but_fixme_should_propagate_errors());

    auto* group = general_tab.find_descendant_of_type_named<GUI::Label>("group");
    group->set_text(String::formatted("{} ({})", group_name, st.st_gid).release_value_but_fixme_should_propagate_errors());

    auto* created_at = general_tab.find_descendant_of_type_named<GUI::Label>("created_at");
    created_at->set_text(MUST(String::from_byte_string(GUI::FileSystemModel::timestamp_string(st.st_ctime))));

    auto* last_modified = general_tab.find_descendant_of_type_named<GUI::Label>("last_modified");
    last_modified->set_text(MUST(String::from_byte_string(GUI::FileSystemModel::timestamp_string(st.st_mtime))));

    auto* owner_read = general_tab.find_descendant_of_type_named<GUI::CheckBox>("owner_read");
    auto* owner_write = general_tab.find_descendant_of_type_named<GUI::CheckBox>("owner_write");
    auto* owner_execute = general_tab.find_descendant_of_type_named<GUI::CheckBox>("owner_execute");
    TRY(setup_permission_checkboxes(*owner_read, *owner_write, *owner_execute, { S_IRUSR, S_IWUSR, S_IXUSR }, m_mode));

    auto* group_read = general_tab.find_descendant_of_type_named<GUI::CheckBox>("group_read");
    auto* group_write = general_tab.find_descendant_of_type_named<GUI::CheckBox>("group_write");
    auto* group_execute = general_tab.find_descendant_of_type_named<GUI::CheckBox>("group_execute");
    TRY(setup_permission_checkboxes(*group_read, *group_write, *group_execute, { S_IRGRP, S_IWGRP, S_IXGRP }, m_mode));

    auto* others_read = general_tab.find_descendant_of_type_named<GUI::CheckBox>("others_read");
    auto* others_write = general_tab.find_descendant_of_type_named<GUI::CheckBox>("others_write");
    auto* others_execute = general_tab.find_descendant_of_type_named<GUI::CheckBox>("others_execute");
    TRY(setup_permission_checkboxes(*others_read, *others_write, *others_execute, { S_IROTH, S_IWOTH, S_IXOTH }, m_mode));

    return {};
}

ErrorOr<void> PropertiesWindow::create_file_type_specific_tabs(GUI::TabWidget& tab_widget)
{
    auto mapped_file_or_error = Core::MappedFile::map(m_path);
    if (mapped_file_or_error.is_error()) {
        warnln("{}: {}", m_path, mapped_file_or_error.release_error());
        return {};
    }
    auto mapped_file = mapped_file_or_error.release_value();

    auto file_name_guess = Core::guess_mime_type_based_on_filename(m_path);
    auto mime_type = Core::guess_mime_type_based_on_sniffed_bytes(mapped_file->bytes()).value_or(file_name_guess);

    // FIXME: Support other archive types
    if (mime_type == "application/zip"sv)
        return create_archive_tab(tab_widget, move(mapped_file));

    if (mime_type.starts_with("audio/"sv))
        return create_audio_tab(tab_widget, move(mapped_file));

    if (mime_type.starts_with("font/"sv) || m_path.ends_with(".font"sv))
        return create_font_tab(tab_widget, move(mapped_file), mime_type);

    if (mime_type.starts_with("image/"sv))
        return create_image_tab(tab_widget, move(mapped_file), mime_type);

    if (mime_type == "application/pdf"sv)
        return create_pdf_tab(tab_widget, move(mapped_file));

    return {};
}

ErrorOr<void> PropertiesWindow::create_archive_tab(GUI::TabWidget& tab_widget, NonnullOwnPtr<Core::MappedFile> mapped_file)
{
    auto maybe_zip = Archive::Zip::try_create(mapped_file->bytes());
    if (!maybe_zip.has_value()) {
        warnln("Failed to read zip file '{}' ", m_path);
        return {};
    }
    auto zip = maybe_zip.release_value();

    auto& tab = tab_widget.add_tab<GUI::Widget>("Archive"_string);
    TRY(tab.load_from_gml(properties_window_archive_tab_gml));

    auto statistics = TRY(zip.calculate_statistics());

    tab.find_descendant_of_type_named<GUI::Label>("archive_file_count")->set_text(String::number(statistics.file_count()));
    tab.find_descendant_of_type_named<GUI::Label>("archive_format")->set_text("ZIP"_string);
    tab.find_descendant_of_type_named<GUI::Label>("archive_directory_count")->set_text(String::number(statistics.directory_count()));
    tab.find_descendant_of_type_named<GUI::Label>("archive_uncompressed_size")->set_text(human_readable_size(statistics.total_uncompressed_bytes()));

    return {};
}

ErrorOr<void> PropertiesWindow::create_audio_tab(GUI::TabWidget& tab_widget, NonnullOwnPtr<Core::MappedFile> mapped_file)
{
    auto loader_or_error = Audio::Loader::create(mapped_file->bytes());
    if (loader_or_error.is_error()) {
        warnln("Failed to open '{}': {}", m_path, loader_or_error.release_error());
        return {};
    }
    auto loader = loader_or_error.release_value();

    auto& tab = tab_widget.add_tab<GUI::Widget>("Audio"_string);
    TRY(tab.load_from_gml(properties_window_audio_tab_gml));

    tab.find_descendant_of_type_named<GUI::Label>("audio_type")->set_text(TRY(String::from_byte_string(loader->format_name())));
    auto duration_seconds = loader->total_samples() / loader->sample_rate();
    tab.find_descendant_of_type_named<GUI::Label>("audio_duration")->set_text(human_readable_digital_time(duration_seconds));
    tab.find_descendant_of_type_named<GUI::Label>("audio_sample_rate")->set_text(TRY(String::formatted("{} Hz", loader->sample_rate())));
    tab.find_descendant_of_type_named<GUI::Label>("audio_format")->set_text(TRY(String::formatted("{}-bit", loader->bits_per_sample())));

    auto channel_count = loader->num_channels();
    String channels_string;
    if (channel_count == 1 || channel_count == 2) {
        channels_string = TRY(String::formatted("{} ({})", channel_count, channel_count == 1 ? "Mono"sv : "Stereo"sv));
    } else {
        channels_string = String::number(channel_count);
    }
    tab.find_descendant_of_type_named<GUI::Label>("audio_channels")->set_text(channels_string);

    tab.find_descendant_of_type_named<GUI::Label>("audio_title")->set_text(loader->metadata().title.value_or({}));
    tab.find_descendant_of_type_named<GUI::Label>("audio_artists")->set_text(TRY(loader->metadata().all_artists()).value_or({}));
    tab.find_descendant_of_type_named<GUI::Label>("audio_album")->set_text(loader->metadata().album.value_or({}));
    tab.find_descendant_of_type_named<GUI::Label>("audio_track_number")
        ->set_text(loader->metadata().track_number.map([](auto number) { return String::number(number); }).value_or({}));
    tab.find_descendant_of_type_named<GUI::Label>("audio_genre")->set_text(loader->metadata().genre.value_or({}));
    tab.find_descendant_of_type_named<GUI::Label>("audio_comment")->set_text(loader->metadata().comment.value_or({}));

    return {};
}

struct FontInfo {
    enum class Format {
        BitmapFont,
        OpenType,
        TrueType,
        WOFF,
        WOFF2,
    };
    Format format;
    NonnullRefPtr<Gfx::Typeface> typeface;
};
static ErrorOr<FontInfo> load_font(StringView path, StringView mime_type, NonnullOwnPtr<Core::MappedFile> mapped_file)
{
    if (path.ends_with(".font"sv)) {
        auto font = TRY(Gfx::BitmapFont::try_load_from_mapped_file(move(mapped_file)));
        auto typeface = TRY(try_make_ref_counted<Gfx::Typeface>(font->family(), font->variant()));
        typeface->add_bitmap_font(move(font));
        return FontInfo { FontInfo::Format::BitmapFont, move(typeface) };
    }

    if (mime_type == "font/otf" || mime_type == "font/ttf") {
        auto font = TRY(OpenType::Font::try_load_from_externally_owned_memory(mapped_file->bytes()));
        auto typeface = TRY(try_make_ref_counted<Gfx::Typeface>(font->family(), font->variant()));
        typeface->set_vector_font(move(font));
        return FontInfo {
            mime_type == "font/otf" ? FontInfo::Format::OpenType : FontInfo::Format::TrueType,
            move(typeface)
        };
    }

    if (mime_type == "font/woff" || mime_type == "font/woff2") {
        auto font = TRY(WOFF::Font::try_load_from_externally_owned_memory(mapped_file->bytes()));
        auto typeface = TRY(try_make_ref_counted<Gfx::Typeface>(font->family(), font->variant()));
        typeface->set_vector_font(move(font));
        return FontInfo {
            mime_type == "font/woff" ? FontInfo::Format::WOFF : FontInfo::Format::WOFF2,
            move(typeface)
        };
    }

    return Error::from_string_view("Unrecognized font format."sv);
}

ErrorOr<void> PropertiesWindow::create_font_tab(GUI::TabWidget& tab_widget, NonnullOwnPtr<Core::MappedFile> mapped_file, StringView mime_type)
{
    auto font_info_or_error = load_font(m_path, mime_type, move(mapped_file));
    if (font_info_or_error.is_error()) {
        warnln("Failed to open '{}': {}", m_path, font_info_or_error.release_error());
        return {};
    }
    auto font_info = font_info_or_error.release_value();
    auto& typeface = font_info.typeface;

    auto& tab = tab_widget.add_tab<GUI::Widget>("Font"_string);
    TRY(tab.load_from_gml(properties_window_font_tab_gml));

    String format_name;
    switch (font_info.format) {
    case FontInfo::Format::BitmapFont:
        format_name = "Bitmap Font"_string;
        break;
    case FontInfo::Format::OpenType:
        format_name = "OpenType"_string;
        break;
    case FontInfo::Format::TrueType:
        format_name = "TrueType"_string;
        break;
    case FontInfo::Format::WOFF:
        format_name = "WOFF"_string;
        break;
    case FontInfo::Format::WOFF2:
        format_name = "WOFF2"_string;
        break;
    }
    tab.find_descendant_of_type_named<GUI::Label>("font_family")->set_text(typeface->family().to_string());
    tab.find_descendant_of_type_named<GUI::Label>("font_fixed_width")->set_text(typeface->is_fixed_width() ? "Yes"_string : "No"_string);
    tab.find_descendant_of_type_named<GUI::Label>("font_format")->set_text(format_name);
    tab.find_descendant_of_type_named<GUI::Label>("font_width")->set_text(TRY(String::from_utf8(Gfx::width_to_name(static_cast<Gfx::FontWidth>(typeface->width())))));

    auto nearest_weight_class_name = [](unsigned weight) {
        if (weight > 925)
            return Gfx::weight_to_name(Gfx::FontWeight::ExtraBlack);
        unsigned weight_class = clamp(round_to<unsigned>(weight / 100.0) * 100, Gfx::FontWeight::Thin, Gfx::FontWeight::Black);
        return Gfx::weight_to_name(weight_class);
    };
    auto weight = typeface->weight();
    tab.find_descendant_of_type_named<GUI::Label>("font_weight")->set_text(TRY(String::formatted("{} ({})", weight, nearest_weight_class_name(weight))));
    tab.find_descendant_of_type_named<GUI::Label>("font_slope")->set_text(TRY(String::from_utf8(Gfx::slope_to_name(typeface->slope()))));

    return {};
}

ErrorOr<void> PropertiesWindow::create_image_tab(GUI::TabWidget& tab_widget, NonnullOwnPtr<Core::MappedFile> mapped_file, StringView mime_type)
{
    auto image_decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(mapped_file->bytes(), mime_type));
    if (!image_decoder)
        return {};

    auto& tab = tab_widget.add_tab<GUI::Widget>("Image"_string);
    TRY(tab.load_from_gml(properties_window_image_tab_gml));

    tab.find_descendant_of_type_named<GUI::Label>("image_type")->set_text(TRY(String::from_utf8(mime_type)));
    tab.find_descendant_of_type_named<GUI::Label>("image_size")->set_text(TRY(String::formatted("{} x {}", image_decoder->width(), image_decoder->height())));

    String animation_text;
    if (image_decoder->is_animated()) {
        auto loops = image_decoder->loop_count();
        auto frames = image_decoder->frame_count();
        StringBuilder builder;
        if (loops == 0) {
            TRY(builder.try_append("Loop indefinitely"sv));
        } else if (loops == 1) {
            TRY(builder.try_append("Once"sv));
        } else {
            TRY(builder.try_appendff("Loop {} times"sv, loops));
        }
        TRY(builder.try_appendff(" ({} frames)"sv, frames));

        animation_text = TRY(builder.to_string());
    } else {
        animation_text = "None"_string;
    }
    tab.find_descendant_of_type_named<GUI::Label>("image_animation")->set_text(move(animation_text));

    auto hide_icc_group = [&tab](String profile_text) {
        tab.find_descendant_of_type_named<GUI::Label>("image_has_icc_profile")->set_text(profile_text);
        tab.find_descendant_of_type_named<GUI::Widget>("image_icc_group")->set_visible(false);
    };

    if (auto embedded_icc_bytes = TRY(image_decoder->icc_data()); embedded_icc_bytes.has_value()) {
        auto icc_profile_or_error = Gfx::ICC::Profile::try_load_from_externally_owned_memory(embedded_icc_bytes.value());
        if (icc_profile_or_error.is_error()) {
            hide_icc_group("Present but invalid"_string);
        } else {
            auto icc_profile = icc_profile_or_error.release_value();

            tab.find_descendant_of_type_named<GUI::Widget>("image_has_icc_line")->set_visible(false);
            tab.find_descendant_of_type_named<GUI::Label>("image_icc_profile")->set_text(icc_profile->tag_string_data(Gfx::ICC::profileDescriptionTag).value_or({}));
            tab.find_descendant_of_type_named<GUI::Label>("image_icc_copyright")->set_text(icc_profile->tag_string_data(Gfx::ICC::copyrightTag).value_or({}));
            tab.find_descendant_of_type_named<GUI::Label>("image_icc_color_space")->set_text(TRY(String::from_utf8(data_color_space_name(icc_profile->data_color_space()))));
            tab.find_descendant_of_type_named<GUI::Label>("image_icc_device_class")->set_text(TRY(String::from_utf8((device_class_name(icc_profile->device_class())))));
        }

    } else {
        hide_icc_group("None"_string);
    }

    auto const& basic_metadata = image_decoder->metadata();
    if (basic_metadata.has_value() && !basic_metadata->main_tags().is_empty()) {
        auto& metadata_group = *tab.find_descendant_of_type_named<GUI::GroupBox>("image_basic_metadata");
        metadata_group.set_visible(true);

        auto const& tags = basic_metadata->main_tags();
        for (auto const& field : tags) {
            auto& widget = metadata_group.add<GUI::Widget>();
            widget.set_layout<GUI::HorizontalBoxLayout>();

            auto& key_label = widget.add<GUI::Label>(String::from_utf8(field.key).release_value_but_fixme_should_propagate_errors());
            key_label.set_text_alignment(Gfx::TextAlignment::TopLeft);
            key_label.set_fixed_width(80);

            auto& value_label = widget.add<GUI::Label>(field.value);
            value_label.set_text_alignment(Gfx::TextAlignment::TopLeft);
        }
    }

    if (auto const& metadata = image_decoder->metadata(); metadata.has_value() && is<Gfx::ExifMetadata>(*metadata)) {
        auto const& exif_metadata = static_cast<Gfx::ExifMetadata const&>(*metadata);
        if (auto gps = Gfx::ExifGPS::from_exif_metadata(exif_metadata); gps.has_value()) {
            auto& gps_container = *tab.find_descendant_of_type_named<GUI::GroupBox>("image_gps");
            gps_container.set_visible(true);

            Maps::MapWidget::Options options {};
            options.center.latitude = gps->latitude();
            options.center.longitude = gps->longitude();
            options.zoom = 14;
            auto& map_widget = gps_container.add<Maps::MapWidget>(options);
            map_widget.add_marker(Maps::MapWidget::Marker {
                .latlng = { gps->latitude(), gps->longitude() },
            });
        }
    }

    return {};
}

ErrorOr<void> PropertiesWindow::create_pdf_tab(GUI::TabWidget& tab_widget, NonnullOwnPtr<Core::MappedFile> mapped_file)
{
    auto maybe_document = PDF::Document::create(mapped_file->bytes());
    if (maybe_document.is_error()) {
        warnln("Failed to open '{}': {}", m_path, maybe_document.error().message());
        return {};
    }
    auto document = maybe_document.release_value();

    if (auto handler = document->security_handler(); handler && !handler->has_user_password()) {
        // FIXME: Show a password dialog, once we've switched to lazy-loading
        auto& tab = tab_widget.add_tab<GUI::Label>("PDF"_string);
        tab.set_text("PDF is password-protected."_string);
        return {};
    }

    if (auto maybe_error = document->initialize(); maybe_error.is_error()) {
        warnln("PDF '{}' seems to be invalid: {}", m_path, maybe_error.error().message());
        return {};
    }

    auto& tab = tab_widget.add_tab<GUI::Widget>("PDF"_string);
    TRY(tab.load_from_gml(properties_window_pdf_tab_gml));

    tab.find_descendant_of_type_named<GUI::Label>("pdf_version")->set_text(TRY(String::formatted("{}.{}", document->version().major, document->version().minor)));
    tab.find_descendant_of_type_named<GUI::Label>("pdf_page_count")->set_text(String::number(document->get_page_count()));

    auto maybe_info_dict = document->info_dict();
    if (maybe_info_dict.is_error()) {
        warnln("Failed to read InfoDict from '{}': {}", m_path, maybe_info_dict.error().message());
    } else if (maybe_info_dict.value().has_value()) {
        auto get_info_string = []<typename T>(PDF::PDFErrorOr<Optional<T>> input) -> T {
            if (input.is_error())
                return T {};
            return input.value().value_or({});
        };

        auto info_dict = maybe_info_dict.release_value().release_value();
        tab.find_descendant_of_type_named<GUI::Label>("pdf_title")->set_text(get_info_string(info_dict.title()));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_author")->set_text(get_info_string(info_dict.author()));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_subject")->set_text(get_info_string(info_dict.subject()));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_keywords")->set_text(get_info_string(info_dict.keywords()));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_creator")->set_text(get_info_string(info_dict.creator()));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_producer")->set_text(get_info_string(info_dict.producer()));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_creation_date")->set_text(TRY(String::from_byte_string((get_info_string(info_dict.creation_date())))));
        tab.find_descendant_of_type_named<GUI::Label>("pdf_modification_date")->set_text(TRY(String::from_byte_string(get_info_string(info_dict.modification_date()))));
    }

    return {};
}

void PropertiesWindow::update()
{
    m_icon->set_bitmap(GUI::FileIconProvider::icon_for_path(make_full_path(m_name), m_mode).bitmap_for_size(32));
    set_title(ByteString::formatted("{} - Properties", m_name));
}

void PropertiesWindow::permission_changed(mode_t mask, bool set)
{
    if (set) {
        m_mode |= mask;
    } else {
        m_mode &= ~mask;
    }

    m_permissions_dirty = m_mode != m_old_mode;
    m_apply_button->set_enabled(m_name_dirty || m_permissions_dirty);
}

ByteString PropertiesWindow::make_full_path(ByteString const& name)
{
    return ByteString::formatted("{}/{}", m_parent_path, name);
}

bool PropertiesWindow::apply_changes()
{
    if (m_name_dirty) {
        ByteString new_name = m_name_box->text();
        ByteString new_file = make_full_path(new_name).characters();

        if (FileSystem::exists(new_file)) {
            GUI::MessageBox::show(this, ByteString::formatted("A file \"{}\" already exists!", new_name), "Error"sv, GUI::MessageBox::Type::Error);
            return false;
        }

        if (rename(make_full_path(m_name).characters(), new_file.characters())) {
            GUI::MessageBox::show(this, ByteString::formatted("Could not rename file: {}!", strerror(errno)), "Error"sv, GUI::MessageBox::Type::Error);
            return false;
        }

        m_name = new_name;
        m_name_dirty = false;
        update();
    }

    if (m_permissions_dirty) {
        if (chmod(make_full_path(m_name).characters(), m_mode)) {
            GUI::MessageBox::show(this, ByteString::formatted("Could not update permissions: {}!", strerror(errno)), "Error"sv, GUI::MessageBox::Type::Error);
            return false;
        }

        m_old_mode = m_mode;
        m_permissions_dirty = false;
    }

    auto directory_view = parent()->find_descendant_of_type_named<FileManager::DirectoryView>("directory_view");
    directory_view->refresh();

    update();
    m_apply_button->set_enabled(false);
    return true;
}

ErrorOr<void> PropertiesWindow::setup_permission_checkboxes(GUI::CheckBox& box_read, GUI::CheckBox& box_write, GUI::CheckBox& box_execute, PermissionMasks masks, mode_t mode)
{
    auto st = TRY(Core::System::lstat(m_path));

    auto can_edit_checkboxes = st.st_uid == getuid();

    box_read.set_checked(mode & masks.read);
    box_read.on_checked = [&, masks](bool checked) { permission_changed(masks.read, checked); };
    box_read.set_enabled(can_edit_checkboxes);

    box_write.set_checked(mode & masks.write);
    box_write.on_checked = [&, masks](bool checked) { permission_changed(masks.write, checked); };
    box_write.set_enabled(can_edit_checkboxes);

    box_execute.set_checked(mode & masks.execute);
    box_execute.on_checked = [&, masks](bool checked) { permission_changed(masks.execute, checked); };
    box_execute.set_enabled(can_edit_checkboxes);

    return {};
}

GUI::Button& PropertiesWindow::make_button(String text, GUI::Widget& parent)
{
    auto& button = parent.add<GUI::Button>(text);
    button.set_fixed_size(70, 22);
    return button;
}

void PropertiesWindow::close()
{
    GUI::Window::close();
    if (m_directory_statistics_calculator)
        m_directory_statistics_calculator->stop();
}

PropertiesWindow::DirectoryStatisticsCalculator::DirectoryStatisticsCalculator(ByteString path)
{
    m_work_queue.enqueue(path);
}

void PropertiesWindow::DirectoryStatisticsCalculator::start()
{
    using namespace AK::TimeLiterals;
    VERIFY(!m_background_action);

    m_background_action = Threading::BackgroundAction<int>::construct(
        [this, strong_this = NonnullRefPtr(*this)](auto& task) -> ErrorOr<int> {
            auto timer = Core::ElapsedTimer();
            while (!m_work_queue.is_empty()) {
                auto base_directory = m_work_queue.dequeue();
                auto result = Core::Directory::for_each_entry(base_directory, Core::DirIterator::SkipParentAndBaseDir, [&](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
                    if (task.is_canceled())
                        return Error::from_errno(ECANCELED);

                    struct stat st = {};
                    if (fstatat(directory.fd(), entry.name.characters(), &st, AT_SYMLINK_NOFOLLOW) < 0) {
                        perror("fstatat");
                        return IterationDecision::Continue;
                    }

                    if (S_ISDIR(st.st_mode)) {
                        auto full_path = LexicalPath::join(directory.path().string(), entry.name).string();
                        m_directory_count++;
                        m_work_queue.enqueue(full_path);
                    } else if (S_ISREG(st.st_mode) || S_ISLNK(st.st_mode)) {
                        m_file_count++;
                        m_total_size_in_bytes += st.st_size;
                    }

                    // Show the first update, then show any subsequent updates every 100ms.
                    if (!task.is_canceled() && on_update && (!timer.is_valid() || timer.elapsed_time() > 100_ms)) {
                        timer.start();
                        on_update(m_total_size_in_bytes, m_file_count, m_directory_count);
                    }

                    return IterationDecision::Continue;
                });
                if (result.is_error() && result.error().code() == ECANCELED)
                    return Error::from_errno(ECANCELED);
            }
            return 0;
        },
        [this](auto) -> ErrorOr<void> {
            if (on_update)
                on_update(m_total_size_in_bytes, m_file_count, m_directory_count);

            return {};
        },
        [](auto) {
            // Ignore the error.
        });
}

void PropertiesWindow::DirectoryStatisticsCalculator::stop()
{
    VERIFY(m_background_action);
    m_background_action->cancel();
}
