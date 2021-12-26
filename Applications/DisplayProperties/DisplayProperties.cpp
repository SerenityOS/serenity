#include <AK/StringBuilder.h>
#include <LibCore/CDirIterator.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GFileSystemModel.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTabWidget.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

#include <Servers/WindowServer/WSWindowManager.h>

#include "DisplayProperties.h"
#include "ItemListModel.h"

DisplayPropertiesWidget::DisplayPropertiesWidget()
    : m_wm_config(CConfigFile::get_for_app("WindowManager"))
{
    create_root_widget();
    create_frame();
    create_resolution_list();
    create_wallpaper_list();
}

void DisplayPropertiesWidget::create_resolution_list()
{
    // TODO: Find a better way to get the default resolution
    m_resolutions.append({ 640, 480 });
    m_resolutions.append({ 800, 600 });
    m_resolutions.append({ 1024, 768 });
    m_resolutions.append({ 1280, 1024 });
    m_resolutions.append({ 1440, 900 });
    m_resolutions.append({ 1600, 900 });
    m_resolutions.append({ 1920, 1080 });
    m_resolutions.append({ 2560, 1080 });

    Size find_size;

    bool okay = false;
    // Let's attempt to find the current resolution and select it!
    find_size.set_width(m_wm_config->read_entry("Screen", "Width", "1024").to_int(okay));
    if (!okay) {
        fprintf(stderr, "DisplayProperties: failed to convert width to int!");
        return;
    }

    find_size.set_height(m_wm_config->read_entry("Screen", "Height", "768").to_int(okay));
    if (!okay) {
        fprintf(stderr, "DisplayProperties: failed to convert height to int!");
        return;
    }

    int index = 0;
    for (auto& resolution : m_resolutions) {
        if (resolution == find_size) {
            m_selected_resolution = m_resolutions.at(index);
            return; // We don't need to do anything else
        }

        index++;
    }

    m_selected_resolution = m_resolutions.at(0);
}

void DisplayPropertiesWidget::create_root_widget()
{
    m_root_widget = new GWidget;
    m_root_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    m_root_widget->set_fill_with_background_color(true);
    m_root_widget->layout()->set_margins({ 4, 4, 4, 16 });
}

void DisplayPropertiesWidget::create_wallpaper_list()
{
    CDirIterator iterator("/res/wallpapers/", CDirIterator::Flags::SkipDots);

    while (iterator.has_next())
        m_wallpapers.append(iterator.next_path());
}

void DisplayPropertiesWidget::create_frame()
{
    auto* tab_widget = new GTabWidget(m_root_widget);

    // First, let's create the "Background" tab
    auto background_splitter = GSplitter::construct(Orientation::Vertical, nullptr);
    tab_widget->add_widget("Wallpaper", background_splitter);

    auto* background_content = new GWidget(background_splitter);
    background_content->set_layout(make<GBoxLayout>(Orientation::Vertical));
    background_content->layout()->set_margins({ 4, 4, 4, 4 });

    m_wallpaper_preview = GLabel::construct(background_splitter);

    auto wallpaper_list = GListView::construct(background_content);
    wallpaper_list->set_background_color(Color::White);
    wallpaper_list->set_model(*ItemListModel<AK::String>::create(m_wallpapers));
    wallpaper_list->horizontal_scrollbar().set_visible(false);
    wallpaper_list->on_selection = [this](auto& index) {
        StringBuilder builder;
        m_selected_wallpaper = m_wallpapers.at(index.row());
        builder.append("/res/wallpapers/");
        builder.append(m_selected_wallpaper);
        m_wallpaper_preview->set_icon(load_png(builder.to_string()));
        m_wallpaper_preview->set_should_stretch_icon(true);
    };

    // Let's add the settings tab
    auto settings_splitter = GSplitter::construct(Orientation::Vertical, nullptr);
    tab_widget->add_widget("Settings", settings_splitter);

    auto* settings_content = new GWidget(settings_splitter);
    settings_content->set_layout(make<GBoxLayout>(Orientation::Vertical));
    settings_content->layout()->set_margins({ 4, 4, 4, 4 });

    auto resolution_list = GListView::construct(settings_content);
    resolution_list->set_background_color(Color::White);
    resolution_list->set_model(*ItemListModel<Size>::create(m_resolutions));
    resolution_list->horizontal_scrollbar().set_visible(false);
    resolution_list->on_selection = [this](auto& index) {
        m_selected_resolution = m_resolutions.at(index.row());
    };

    settings_content->layout()->add_spacer();

    // Add the apply and cancel buttons
    auto* bottom_widget = new GWidget(m_root_widget);
    bottom_widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    bottom_widget->layout()->add_spacer();
    bottom_widget->set_size_policy(Orientation::Vertical, SizePolicy::Fixed);
    bottom_widget->set_preferred_size(1, 22);

    auto* apply_button = new GButton(bottom_widget);
    apply_button->set_text("Apply");
    apply_button->set_size_policy(Orientation::Vertical, SizePolicy::Fixed);
    apply_button->set_size_policy(Orientation::Horizontal, SizePolicy::Fixed);
    apply_button->set_preferred_size(60, 22);
    apply_button->on_click = [this, tab_widget](GButton&) {
        send_settings_to_window_server(tab_widget->active_tab_index());
    };

    auto* ok_button = new GButton(bottom_widget);
    ok_button->set_text("OK");
    ok_button->set_size_policy(Orientation::Vertical, SizePolicy::Fixed);
    ok_button->set_size_policy(Orientation::Horizontal, SizePolicy::Fixed);
    ok_button->set_preferred_size(60, 22);
    ok_button->on_click = [this, tab_widget](GButton&) {
        send_settings_to_window_server(tab_widget->active_tab_index());
        GApplication::the().quit();
    };

    auto* cancel_button = new GButton(bottom_widget);
    cancel_button->set_text("Cancel");
    cancel_button->set_size_policy(Orientation::Vertical, SizePolicy::Fixed);
    cancel_button->set_size_policy(Orientation::Horizontal, SizePolicy::Fixed);
    cancel_button->set_preferred_size(60, 22);
    cancel_button->on_click = [this](GButton&) {
        GApplication::the().quit();
    };
}

void DisplayPropertiesWidget::send_settings_to_window_server(int tab_index)
{
    if (tab_index == TabIndices::Wallpaper) {
        StringBuilder builder;
        builder.append("/res/wallpapers/");
        builder.append(m_selected_wallpaper);
        GDesktop::the().set_wallpaper(builder.to_string());
    } else if (tab_index == TabIndices::Settings) {
        WSAPI_ClientMessage request;
        request.type = WSAPI_ClientMessage::Type::SetResolution;
        dbg() << "Attempting to set resolution " << m_selected_resolution;
        request.wm_conf.resolution = { m_selected_resolution.width(), m_selected_resolution.height() };
        auto response = GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidSetResolution);
        ASSERT(response.value == 1);
    } else {
        dbg() << "Invalid tab index " << tab_index;
    }
}
