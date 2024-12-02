/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include "ThumbnailsModel.h"
#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/Variant.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Forward.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibPDF/Document.h>

class PagedErrorsModel : public GUI::Model {

    enum Columns {
        Page = 0,
        Message,
        _Count
    };

    using PageErrors = AK::OrderedHashTable<ByteString>;
    using PagedErrors = HashMap<u32, PageErrors>;

public:
    int row_count(GUI::ModelIndex const& index) const override
    {
        // There are two levels: number of pages and number of errors in page
        if (!index.is_valid()) {
            return static_cast<int>(m_paged_errors.size());
        }
        if (!index.parent().is_valid()) {
            return static_cast<int>(error_count_in_page(index));
        }
        return 0;
    }

    int column_count(GUI::ModelIndex const&) const override
    {
        return Columns::_Count;
    }

    int tree_column() const override
    {
        return Columns::Page;
    }

    ErrorOr<String> column_name(int index) const override
    {
        switch (index) {
        case 0:
            return "Page"_string;
        case 1:
            return "Message"_string;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent) const override
    {
        if (!parent.is_valid()) {
            return create_index(row, column);
        }
        auto const& page = m_pages_with_errors[parent.row()];
        return create_index(row, column, &page);
    }

    GUI::ModelIndex parent_index(GUI::ModelIndex const& index) const override
    {
        auto* const internal_data = index.internal_data();
        if (internal_data == nullptr)
            return {};
        auto page = *static_cast<u32 const*>(internal_data);
        auto page_idx = static_cast<int>(m_pages_with_errors.find_first_index(page).release_value());
        return create_index(page_idx, index.column());
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole) const override
    {
        if (!index.parent().is_valid()) {
            switch (index.column()) {
            case Columns::Page:
                return m_pages_with_errors[index.row()] + 1;
            case Columns::Message:
                return ByteString::formatted("{} errors", error_count_in_page(index));
            default:
                VERIFY_NOT_REACHED();
            }
        }

        auto page = *static_cast<u32 const*>(index.internal_data());
        switch (index.column()) {
        case Columns::Page:
            return "";
        case Columns::Message: {
            auto page_errors = m_paged_errors.get(page).release_value();
            // dbgln("Errors on page {}: {}. Requesting data for index {},{}", page, page_errors.size(), index.row(), index.column());
            auto it = page_errors.begin();
            auto row = index.row();
            for (int i = 0; i < row; ++i, ++it)
                ;
            return *it;
        }
        }
        VERIFY_NOT_REACHED();
    }

    void add_errors(u32 page, PDF::Errors const& errors)
    {
        auto old_size = total_error_count();
        if (!m_pages_with_errors.contains_slow(page)) {
            m_pages_with_errors.append(page);
        }
        auto& page_errors = m_paged_errors.ensure(page);
        for (auto const& error : errors.errors())
            page_errors.set(error.message());
        auto new_size = total_error_count();
        if (old_size != new_size)
            invalidate();
    }

private:
    size_t total_error_count() const
    {
        size_t count = 0;
        for (auto const& entry : m_paged_errors)
            count += entry.value.size();
        return count;
    }

    size_t error_count_in_page(GUI::ModelIndex const& index) const
    {
        VERIFY(!index.parent().is_valid());
        auto page = m_pages_with_errors[index.row()];
        return m_paged_errors.get(page).release_value().size();
    }

    Vector<u32> m_pages_with_errors;
    PagedErrors m_paged_errors;
};

PDFViewerWidget::PDFViewerWidget()
    : m_paged_errors_model(adopt_ref(*new PagedErrorsModel()))
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    auto& toolbar_container = add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

    auto& h_splitter = add<GUI::HorizontalSplitter>();
    h_splitter.layout()->set_spacing(4);

    m_sidebar = h_splitter.add<SidebarWidget>();
    m_sidebar->set_preferred_width(200);
    m_sidebar->set_visible(false);
    m_sidebar->on_destination_selected = [&](PDF::Destination const& destination) {
        auto maybe_page = destination.page;
        if (!maybe_page.has_value())
            return;
        auto page = maybe_page.release_value();
        m_viewer->set_current_page(page);
        m_page_text_box->set_value(m_viewer->current_page() + 1);
    };

    m_vertical_splitter = h_splitter.add<GUI::VerticalSplitter>();
    m_vertical_splitter->layout()->set_spacing(4);

    m_viewer = m_vertical_splitter->add<PDFViewer>();
    m_viewer->on_page_change = [&](auto new_page) {
        m_page_text_box->set_value(new_page + 1, GUI::AllowCallback::No);
        m_go_to_prev_page_action->set_enabled(new_page > 0);
        m_go_to_next_page_action->set_enabled(new_page < m_viewer->document()->get_page_count() - 1);
    };
    m_viewer->on_render_errors = [&](u32 page, PDF::Errors const& errors) {
        m_paged_errors_model->add_errors(page, errors);
    };

    m_errors_tree_view = GUI::TreeView::construct();
    m_errors_tree_view->set_preferred_height(10);
    m_errors_tree_view->column_header().set_visible(true);
    m_errors_tree_view->set_should_fill_selected_rows(true);
    m_errors_tree_view->set_selection_behavior(GUI::AbstractView::SelectionBehavior::SelectRows);
    m_errors_tree_view->set_model(MUST(GUI::SortingProxyModel::create(m_paged_errors_model)));
    m_errors_tree_view->set_key_column(0);

    if (m_viewer->show_rendering_diagnostics()) {
        m_vertical_splitter->add_child(*m_errors_tree_view);
    }

    initialize_toolbar(toolbar);
}

ErrorOr<void> PDFViewerWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_open_action([&](auto&) {
        FileSystemAccessClient::OpenFileOptions options {
            .allowed_file_types = Vector {
                GUI::FileTypeFilter { "PDF Files", { { "pdf" } } },
                GUI::FileTypeFilter::all_files(),
            },
        };
        auto response = FileSystemAccessClient::Client::the().open_file(&window, options);
        if (!response.is_error())
            open_file(response.value().filename(), response.value().release_stream());
    }));
    file_menu->add_separator();
    file_menu->add_recent_files_list([&](auto& action) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(&window, action.text());
        if (!response.is_error())
            open_file(response.value().filename(), response.value().release_stream());
    });
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto view_menu = window.add_menu("&View"_string);
    view_menu->add_action(*m_toggle_sidebar_action);
    view_menu->add_separator();
    auto view_mode_menu = view_menu->add_submenu("View &Mode"_string);
    view_mode_menu->add_action(*m_page_view_mode_single);
    view_mode_menu->add_action(*m_page_view_mode_multiple);
    view_menu->add_separator();
    view_menu->add_action(*m_zoom_in_action);
    view_menu->add_action(*m_zoom_out_action);
    view_menu->add_action(*m_reset_zoom_action);

    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    auto debug_menu = window.add_menu("&Debug"_string);
    auto toggle_show_diagnostics = GUI::Action::create_checkable("Show Rendering &Diagnostics", [&](auto& action) {
        if (action.is_checked()) {
            m_vertical_splitter->add_child(*m_errors_tree_view);
        } else {
            m_vertical_splitter->remove_child(*m_errors_tree_view);
        }
        m_viewer->set_show_rendering_diagnostics(action.is_checked());
    });
    toggle_show_diagnostics->set_checked(m_viewer->show_rendering_diagnostics());
    debug_menu->add_action(toggle_show_diagnostics);
    auto toggle_show_clipping_paths = GUI::Action::create_checkable("Show &Clipping Paths", [&](auto& action) {
        m_viewer->set_show_clipping_paths(action.is_checked());
    });
    toggle_show_clipping_paths->set_checked(m_viewer->show_clipping_paths());
    debug_menu->add_action(toggle_show_clipping_paths);
    auto toggle_show_images = GUI::Action::create_checkable("Show &Images", [&](auto& action) {
        m_viewer->set_show_images(action.is_checked());
    });
    toggle_show_images->set_checked(m_viewer->show_images());
    debug_menu->add_action(toggle_show_images);
    auto toggle_show_hidden_text = GUI::Action::create_checkable("Show &Hidden Text", [&](auto& action) {
        m_viewer->set_show_hidden_text(action.is_checked());
    });
    toggle_show_hidden_text->set_checked(m_viewer->show_hidden_text());
    debug_menu->add_action(toggle_show_hidden_text);
    auto toggle_clip_images = GUI::Action::create_checkable("Clip I&mages", [&](auto& action) {
        m_viewer->set_clip_images(action.is_checked());
    });
    toggle_clip_images->set_checked(m_viewer->clip_images());
    debug_menu->add_action(toggle_clip_images);
    auto toggle_clip_paths = GUI::Action::create_checkable("Clip &Paths", [&](auto& action) {
        m_viewer->set_clip_paths(action.is_checked());
    });
    toggle_clip_paths->set_checked(m_viewer->clip_paths());
    debug_menu->add_action(toggle_clip_paths);
    auto toggle_clip_text = GUI::Action::create_checkable("Clip &Text", [&](auto& action) {
        m_viewer->set_clip_text(action.is_checked());
    });
    toggle_clip_text->set_checked(m_viewer->clip_text());
    debug_menu->add_action(toggle_clip_text);

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_about_action("PDF Viewer"_string, GUI::Icon::default_icon("app-pdf-viewer"sv), &window));
    return {};
}

NonnullRefPtr<Gfx::Bitmap const> PDFViewerWidget::render_thumbnail_for_rendered_page(u32 page_index)
{
    int rect_square_size = 96;
    Gfx::IntSize thumbnail_size(0, 0);
    auto rendered_page = m_viewer->get_rendered_page(page_index).value();

    float width_mult = rect_square_size / float(rendered_page->width());
    float height_mult = rect_square_size / float(rendered_page->height());
    float resolved_mult = rendered_page->width() < rendered_page->height() ? height_mult : width_mult;

    thumbnail_size.set_width(rendered_page->width() * resolved_mult);
    thumbnail_size.set_height(rendered_page->height() * resolved_mult);

    return rendered_page->scaled_to_size(thumbnail_size).release_value_but_fixme_should_propagate_errors();
}

void PDFViewerWidget::reset_thumbnails()
{
    auto model = m_sidebar->thumbnails_list_view()->model();
    (void)static_cast<ThumbnailsModel*>(model)->reset_thumbnails(m_viewer->document()->get_page_count());
}

void PDFViewerWidget::select_thumbnail(u32 page_index)
{
    m_sidebar->thumbnails_list_view()->select_list_item(page_index);
}

NonnullRefPtr<Gfx::Bitmap const> PDFViewerWidget::update_thumbnail_for_page(u32 page_index)
{
    auto model = m_sidebar->thumbnails_list_view()->model();
    auto thumbnail = render_thumbnail_for_rendered_page(page_index);
    static_cast<ThumbnailsModel*>(model)->update_thumbnail(page_index, thumbnail);
    return thumbnail;
}

void PDFViewerWidget::initialize_toolbar(GUI::Toolbar& toolbar)
{
    auto open_outline_action = GUI::Action::create(
        "Toggle &Sidebar", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/sidebar.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            m_sidebar_open = !m_sidebar_open;
            m_sidebar->set_visible(m_sidebar_open);
        },
        nullptr);
    open_outline_action->set_enabled(false);
    m_toggle_sidebar_action = open_outline_action;

    toolbar.add_action(*open_outline_action);
    toolbar.add_separator();

    m_go_to_prev_page_action = GUI::Action::create("Go to &Previous Page", Gfx::Bitmap::load_from_file("/res/icons/16x16/go-up.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        VERIFY(m_viewer->current_page() > 0);
        m_page_text_box->set_value(m_viewer->current_page());
    });
    m_go_to_prev_page_action->set_enabled(false);

    m_go_to_next_page_action = GUI::Action::create("Go to &Next Page", Gfx::Bitmap::load_from_file("/res/icons/16x16/go-down.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        VERIFY(m_viewer->current_page() < m_viewer->document()->get_page_count() - 1);
        m_page_text_box->set_value(m_viewer->current_page() + 2);
    });
    m_go_to_next_page_action->set_enabled(false);

    toolbar.add_action(*m_go_to_prev_page_action);
    toolbar.add_action(*m_go_to_next_page_action);

    m_page_text_box = toolbar.add<GUI::NumericInput>();
    m_page_text_box->set_enabled(false);
    m_page_text_box->set_fixed_width(30);
    m_page_text_box->set_min(1);

    m_page_text_box->on_number_changed = [&](i64 number) {
        auto page_count = m_viewer->document()->get_page_count();
        auto new_page_number = static_cast<u32>(number);
        VERIFY(new_page_number >= 1 && new_page_number <= page_count);
        m_viewer->set_current_page(new_page_number - 1);
        m_go_to_prev_page_action->set_enabled(new_page_number > 1);
        m_go_to_next_page_action->set_enabled(new_page_number < page_count);
        select_thumbnail(m_viewer->current_page());
    };

    m_total_page_label = toolbar.add<GUI::Label>();
    m_total_page_label->set_autosize(true, 5);
    toolbar.add_separator();

    m_zoom_in_action = GUI::CommonActions::make_zoom_in_action([&](auto&) {
        m_viewer->zoom_in();
    });

    m_zoom_out_action = GUI::CommonActions::make_zoom_out_action([&](auto&) {
        m_viewer->zoom_out();
    });

    m_reset_zoom_action = GUI::CommonActions::make_reset_zoom_action([&](auto&) {
        m_viewer->reset_zoom();
    });

    m_rotate_counterclockwise_action = GUI::CommonActions::make_rotate_counterclockwise_action([&](auto&) {
        m_viewer->rotate(-90);
        reset_thumbnails();
    });

    m_rotate_clockwise_action = GUI::CommonActions::make_rotate_clockwise_action([&](auto&) {
        m_viewer->rotate(90);
        reset_thumbnails();
    });

    m_zoom_in_action->set_enabled(false);
    m_zoom_out_action->set_enabled(false);
    m_reset_zoom_action->set_enabled(false);
    m_rotate_counterclockwise_action->set_enabled(false);
    m_rotate_clockwise_action->set_enabled(false);

    m_page_view_mode_single = GUI::Action::create_checkable("Single", [&](auto&) {
        m_viewer->set_page_view_mode(PDFViewer::PageViewMode::Single);
    });
    m_page_view_mode_single->set_status_tip("Show single page at a time"_string);

    m_page_view_mode_multiple = GUI::Action::create_checkable("Multiple", [&](auto&) {
        m_viewer->set_page_view_mode(PDFViewer::PageViewMode::Multiple);
    });
    m_page_view_mode_multiple->set_status_tip("Show multiple pages at a time"_string);

    if (m_viewer->page_view_mode() == PDFViewer::PageViewMode::Single) {
        m_page_view_mode_single->set_checked(true);
    } else {
        m_page_view_mode_multiple->set_checked(true);
    }

    m_page_view_action_group.add_action(*m_page_view_mode_single);
    m_page_view_action_group.add_action(*m_page_view_mode_multiple);
    m_page_view_action_group.set_exclusive(true);
    toolbar.add_action(*m_page_view_mode_single);
    toolbar.add_action(*m_page_view_mode_multiple);
    toolbar.add_separator();

    toolbar.add_action(*m_zoom_in_action);
    toolbar.add_action(*m_zoom_out_action);
    toolbar.add_action(*m_reset_zoom_action);
    toolbar.add_action(*m_rotate_counterclockwise_action);
    toolbar.add_action(*m_rotate_clockwise_action);
    toolbar.add_separator();
}

void PDFViewerWidget::open_file(StringView path, NonnullOwnPtr<Core::File> file)
{
    auto maybe_error = try_open_file(path, move(file));
    if (maybe_error.is_error()) {
        auto error = maybe_error.release_error();
        warnln("{}", error.message());
        auto user_error_message = ByteString::formatted("Failed to load the document. Error:\n{}.", error.message());
        GUI::MessageBox::show_error(nullptr, user_error_message.view());
    }
}

PDF::PDFErrorOr<void> PDFViewerWidget::try_open_file(StringView path, NonnullOwnPtr<Core::File> file)
{
    window()->set_title(ByteString::formatted("{} - PDF Viewer", path));

    m_buffer = TRY(file->read_until_eof());
    auto document = TRY(PDF::Document::create(m_buffer));

    if (auto sh = document->security_handler(); sh && !sh->has_user_password()) {
        String password;
        while (true) {
            auto result = GUI::InputBox::show(window(), password, "Password"sv, "Password required"sv, GUI::InputType::Password);
            if (result == GUI::Dialog::ExecResult::OK
                && document->security_handler()->try_provide_user_password(password))
                break;
            if (result == GUI::Dialog::ExecResult::Cancel)
                return {};
        }
    }

    TRY(document->initialize());
    TRY(m_viewer->set_document(document));

    m_total_page_label->set_text(TRY(String::formatted("of {}", document->get_page_count())));

    m_page_text_box->set_enabled(true);
    m_page_text_box->set_value(1, GUI::AllowCallback::No);
    m_page_text_box->set_max(document->get_page_count());
    m_go_to_prev_page_action->set_enabled(false);
    m_go_to_next_page_action->set_enabled(document->get_page_count() > 1);
    m_toggle_sidebar_action->set_enabled(true);
    m_zoom_in_action->set_enabled(true);
    m_zoom_out_action->set_enabled(true);
    m_reset_zoom_action->set_enabled(true);
    m_rotate_counterclockwise_action->set_enabled(true);
    m_rotate_clockwise_action->set_enabled(true);

    if (document->outline()) {
        auto outline = document->outline();
        TRY(m_sidebar->set_outline(outline.release_nonnull()));
        m_sidebar->set_visible(true);
        m_sidebar_open = true;
    } else {
        TRY(m_sidebar->set_outline({}));
        m_sidebar->set_visible(false);
        m_sidebar_open = false;
    }

    auto thumbnails_model = ThumbnailsModel::create();
    m_sidebar->thumbnails_list_view()->set_model(thumbnails_model);
    reset_thumbnails();

    m_sidebar->thumbnails_list_view()->on_selection_change = [this] {
        auto page_index = m_sidebar->thumbnails_list_view()->selection().first().row();
        if (page_index >= 0) {
            m_viewer->set_current_page(page_index);
            m_page_text_box->set_value(page_index + 1);
        }
    };

    select_thumbnail(m_viewer->current_page());

    GUI::Application::the()->set_most_recently_open_file(path);

    return {};
}

void PDFViewerWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void PDFViewerWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        if (urls.size() > 1) {
            GUI::MessageBox::show(window(), "PDF Viewer can only open one file at a time!"sv, "One at a time please!"sv, GUI::MessageBox::Type::Error);
            return;
        }

        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window(), URL::percent_decode(urls.first().serialize_path()));
        if (response.is_error())
            return;
        if (auto result = try_open_file(response.value().filename(), response.value().release_stream()); result.is_error())
            GUI::MessageBox::show(window(), "Unable to open file.\n"sv, "Error"sv, GUI::MessageBox::Type::Error);
    }
}
