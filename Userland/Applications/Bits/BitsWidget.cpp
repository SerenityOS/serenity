/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitsWidget.h"
#include "GeneralTorrentInfoWidget.h"
#include <AK/NumberFormat.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/Splitter.h>

int TorrentModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

ErrorOr<String> TorrentModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name"_short_string;
    case Column::Size:
        return "Size"_short_string;
    case Column::State:
        return TRY("State"_string);
    case Column::Progress:
        return TRY("Progress"_string);
    case Column::DownloadSpeed:
        return TRY("Download Speed"_string);
    case Column::UploadSpeed:
        return TRY("Upload Speed"_string);
    case Column::Path:
        return TRY("Path"_string);
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant TorrentModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;
    if (role == GUI::ModelRole::Display) {
        auto torrent = torrent_at(index.row());

        switch (index.column()) {
        case Column::Name:
            return torrent.display_name;
        case Column::Size:
            return AK::human_readable_quantity(torrent.size);
        case Column::State:
            return state_to_string(torrent.state);
        case Column::Progress:
            return DeprecatedString::formatted("{:.1}%", torrent.state == BitTorrent::TorrentState::CHECKING ? torrent.check_progress : torrent.progress);
        case Column::DownloadSpeed:
            return DeprecatedString::formatted("{}/s", human_readable_size(torrent.download_speed));
        case Column::UploadSpeed:
            return DeprecatedString::formatted("{}/s", human_readable_size(torrent.upload_speed));
        case Column::Path:
            return torrent.save_path;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return {};
}

int TorrentModel::row_count(GUI::ModelIndex const&) const
{
    return m_torrents->size();
}

BitTorrent::TorrentView TorrentModel::torrent_at(int index) const
{
    return m_torrents->get(m_hashes.at(index)).release_value();
}

void TorrentModel::update(NonnullOwnPtr<HashMap<BitTorrent::InfoHash, BitTorrent::TorrentView>> torrents)
{
    m_torrents = move(torrents);
    m_hashes = m_torrents->keys();
    did_update(UpdateFlag::DontInvalidateIndices);
}

void BitsWidget::open_file(String const& filename, NonnullOwnPtr<Core::File> file, bool start)
{
    dbgln("Opening file {}", filename);
    auto maybe_meta_info = BitTorrent::MetaInfo::create(*file);
    file->close();

    if (maybe_meta_info.is_error()) {
        GUI::MessageBox::show_error(this->window(), String::formatted("Error parsing torrent file: {}", maybe_meta_info.error()).release_value());
        return;
    }

    auto meta_info = maybe_meta_info.release_value();
    auto info_hash = BitTorrent::InfoHash(meta_info->info_hash());
    m_engine->add_torrent(move(meta_info), Core::StandardPaths::downloads_directory());

    if (start)
        m_engine->start_torrent(info_hash);
}

ErrorOr<NonnullRefPtr<BitsWidget>> BitsWidget::create(NonnullRefPtr<BitTorrent::Engine> engine, GUI::Window* window)
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) BitsWidget(move(engine))));

    widget->set_layout<GUI::VerticalBoxLayout>();

    auto& file_menu = window->add_menu("&File"_short_string);

    file_menu.add_action(GUI::CommonActions::make_open_action([window, widget](auto&) {
        FileSystemAccessClient::OpenFileOptions options {
            .window_title = "Open a torrent file"sv,
            .path = Core::StandardPaths::home_directory(),
            .requested_access = Core::File::OpenMode::Read,
            .allowed_file_types = { { GUI::FileTypeFilter { "Torrent Files", { { "torrent" } } }, GUI::FileTypeFilter::all_files() } }
        };
        auto maybe_file = FileSystemAccessClient::Client::the().open_file(window, options);
        if (maybe_file.is_error()) {
            dbgln("err: {}", maybe_file.error());
            return;
        }

        widget->open_file(maybe_file.value().filename(), maybe_file.value().release_stream(), false);
    }));

    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        GUI::Application::the()->quit();
    }));

    auto start_torrent_action = GUI::Action::create("Start",
        [widget](GUI::Action&) {
            widget->m_torrents_table_view->selection().for_each_index([widget](GUI::ModelIndex const& index) {
                widget->m_engine->start_torrent(widget->m_torrent_model->torrent_at(index.row()).info_hash);
            });
        });

    auto stop_torrent_action = GUI::Action::create("Stop",
        [widget](GUI::Action&) {
            widget->m_torrents_table_view->selection().for_each_index([widget](GUI::ModelIndex const& index) {
                widget->m_engine->stop_torrent(widget->m_torrent_model->torrent_at(index.row()).info_hash);
            });
        });

    auto cancel_checking_torrent_action = GUI::Action::create("Cancel checking",
        [widget](GUI::Action&) {
            widget->m_torrents_table_view->selection().for_each_index([widget](GUI::ModelIndex const& index) {
                widget->m_engine->cancel_checking(widget->m_torrent_model->torrent_at(index.row()).info_hash);
            });
        });

    auto& main_splitter = widget->add<GUI::VerticalSplitter>();
    main_splitter.layout()->set_spacing(4);

    widget->m_torrent_model = make_ref_counted<TorrentModel>();
    widget->m_torrents_table_view = main_splitter.add<GUI::TableView>();
    widget->m_torrents_table_view->set_model(widget->m_torrent_model);
    widget->m_torrents_table_view->set_selection_mode(GUI::AbstractView::SelectionMode::MultiSelection);

    widget->m_torrents_table_view->on_context_menu_request = [widget, start_torrent_action, stop_torrent_action, cancel_checking_torrent_action](GUI::ModelIndex const& model_index, GUI::ContextMenuEvent const& event) {
        if (model_index.is_valid()) {
            widget->m_torrent_context_menu = GUI::Menu::construct();
            BitTorrent::TorrentState state = widget->m_torrent_model->torrent_at(model_index.row()).state;
            if (state == BitTorrent::TorrentState::STOPPED || state == BitTorrent::TorrentState::ERROR)
                widget->m_torrent_context_menu->add_action(start_torrent_action);
            else if (state == BitTorrent::TorrentState::STARTED || state == BitTorrent::TorrentState::SEEDING)
                widget->m_torrent_context_menu->add_action(stop_torrent_action);
            else if (state == BitTorrent::TorrentState::CHECKING)
                widget->m_torrent_context_menu->add_action(cancel_checking_torrent_action);

            widget->m_torrent_context_menu->popup(event.screen_position());
        }
    };

    widget->m_bottom_tab_widget = main_splitter.add<GUI::TabWidget>();
    widget->m_bottom_tab_widget->set_preferred_height(14);
    widget->m_general_widget = TRY(widget->m_bottom_tab_widget->try_add_tab<GeneralTorrentInfoWidget>(TRY("General"_string)));
    widget->m_peer_list_widget = widget->m_bottom_tab_widget->add_tab<PeerListWidget>("Peers"_short_string);

    auto selected_torrent = [widget]() -> Optional<BitTorrent::TorrentView> {
        int selected_index = widget->m_torrents_table_view->selection().first().row();
        if (selected_index >= 0)
            return widget->m_torrent_model->torrent_at(selected_index);
        else
            return {};
    };

    auto update_general_widget = [widget, selected_torrent] {
        widget->m_general_widget->update(selected_torrent());
    };

    auto update_peer_list_widget = [widget, selected_torrent] {
        auto peers = selected_torrent().map([&](auto torrent) -> auto { return torrent.peers; }).value_or({});
        widget->m_peer_list_widget->update(peers);
    };

    widget->m_torrents_table_view->on_selection_change = [update_peer_list_widget, update_general_widget] {
        update_general_widget();
        update_peer_list_widget();
    };

    widget->m_engine->register_views_update_callback(200, [&, widget, &event_loop = Core::EventLoop::current(), update_general_widget, update_peer_list_widget](NonnullOwnPtr<HashMap<BitTorrent::InfoHash, BitTorrent::TorrentView>> torrents) {
        event_loop.deferred_invoke([&, widget, update_general_widget, update_peer_list_widget, torrents = move(torrents)]() mutable {
            u64 progress = 0;
            for (auto const& torrent : *torrents) {
                progress += torrent.value.progress;
            }
            widget->window()->set_progress(torrents->size() > 0 ? progress / torrents->size() : 0);
            widget->m_torrent_model->update(move(torrents));
            update_general_widget();
            update_peer_list_widget();
        });
    });

    return widget;
}

BitsWidget::BitsWidget(NonnullRefPtr<BitTorrent::Engine> engine)
    : m_engine(move(engine))
{
}
