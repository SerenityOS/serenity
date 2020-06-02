/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include "NetworkHistoryWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Browser {

NetworkHistoryWidget::NetworkHistoryWidget()
{
    set_fill_with_background_color(true);

    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });
    layout()->set_spacing(4);

    auto& toolbar_container = add<GUI::ToolBarContainer>();
    auto& toolbar = toolbar_container.add<GUI::ToolBar>();

    auto& paused = toolbar.add<GUI::CheckBox>("Paused");
    paused.on_checked = [this](bool checked) { m_paused = checked; };
    paused.set_checked(m_paused);
    paused.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    paused.set_preferred_size(61, 26);

    auto& clear_on_navigation = toolbar.add<GUI::CheckBox>("Clear on navigation");
    clear_on_navigation.on_checked = [this](bool checked) { m_clear_on_navigation = checked; };
    clear_on_navigation.set_checked(m_clear_on_navigation);
    clear_on_navigation.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    clear_on_navigation.set_preferred_size(131, 26);

    auto& disable_cache = toolbar.add<GUI::CheckBox>("Disable cache (while open)");
    disable_cache.on_checked = [this](bool checked) { m_cache_disabled = checked; };
    disable_cache.set_checked(m_cache_disabled);
    disable_cache.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    disable_cache.set_preferred_size(166, 26);

    auto& splitter = add<GUI::VerticalSplitter>();

    m_history_view = splitter.add<GUI::TableView>();
    m_history_view->set_model(Web::NetworkHistoryModel::create(m_network_history));

    // Path can be very long, so it's hidden by default.
    m_history_view->set_column_hidden(Web::NetworkHistoryModel::Column::Path, true);

    m_context_menu = GUI::Menu::construct();

    m_context_menu->add_action(GUI::Action::create("Open in new tab", [this](auto&) {
        if (on_tab_open_request)
            on_tab_open_request(m_context_menu_url);
    }));

    m_history_view->on_context_menu_request = [this](const auto& index, const auto& event) {
        if (!index.is_valid())
            return;

        m_context_menu_url = m_history_view->model()->data(index, GUI::Model::Role::Custom).to_string();
        m_context_menu->popup(event.screen_position());
    };
}

NetworkHistoryWidget::~NetworkHistoryWidget()
{
    unregister_callbacks();
}

void NetworkHistoryWidget::update_view()
{
    m_history_view->model()->update();
}

void NetworkHistoryWidget::register_callbacks()
{
    // FIXME: Resource loader can't tell which tab the load came from,
    //        so all tabs will report to the most recently opened window.
    Web::ResourceLoader::the().on_load = [this](auto load_id, auto& url) {
        if (m_paused)
            return;

        Web::NetworkHistoryModel::Entry new_entry;
        new_entry.url = url;
        new_entry.load_timer.start();

        m_network_history.set(load_id, new_entry);

        // This will add some microseconds to the load timer, which isn't that big of a deal.
        update_view();
    };

    Web::ResourceLoader::the().on_load_finish = [this](auto load_id, bool success, bool cached) {
        auto& entry = m_network_history.get(load_id).value();

        entry.time = entry.load_timer.elapsed();
        entry.complete = true;
        entry.success = success;
        entry.cached = cached;

        m_network_history.set(load_id, entry);

        update_view();
    };

    Web::ResourceLoader::the().cache_disabled_check = [this] { return m_cache_disabled; };
}

void NetworkHistoryWidget::unregister_callbacks()
{
    // FIXME: If you open two or more windows and close one, the rest will stop working.
    Web::ResourceLoader::the().on_load = nullptr;
    Web::ResourceLoader::the().on_load_finish = nullptr;
    Web::ResourceLoader::the().cache_disabled_check = nullptr;
}

void NetworkHistoryWidget::on_page_navigation()
{
    if (!m_clear_on_navigation)
        return;

    m_network_history.clear();
    update_view();
}

}
