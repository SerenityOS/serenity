/*
 * Copyright (c) 2021, FrHun <frhun@t-online.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PropertiesWindowHashesTab.h"
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <Applications/FileManager/PropertiesWindowHashesHashWidgetGML.h>
#include <Applications/FileManager/PropertiesWindowHashesTabGML.h>
#include <LibCore/IODevice.h>
#include <LibGUI/Clipboard.h>
#include <LibThreading/BackgroundAction.h>
#include <unistd.h>

HashTab::HashTab(String const& path, bool calculate_on_show)
    : m_path(path)
    , m_calculate_on_show(calculate_on_show)
{
    load_from_gml(properties_window_hashes_tab_gml);

    m_icon_hash_matching = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/hash-correct.png");
    VERIFY(!m_icon_hash_matching.is_null());
    m_icon_hash_not_matching = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/hash-mismatch.png");
    VERIFY(!m_icon_hash_not_matching.is_null());
    m_icon_hash_load = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/hash-ask.png");
    VERIFY(!m_icon_hash_load.is_null());
    m_icon_hash = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/hash.png");
    VERIFY(!m_icon_hash.is_null());
    m_icon_copy = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png");
    VERIFY(!m_icon_copy.is_null());

    Widget* spacer = find_descendant_of_type_named<GUI::Widget>("spacer");

    {
        auto md5_widget = HashWidget::construct(this->make_weak_ptr<HashTab>(), String("MD5:"), Crypto::Hash::HashKind::MD5);
        m_hash_widgets.append({ md5_widget });
        insert_child_before(md5_widget, *spacer);
    }
    {
        auto sha1_widget = HashWidget::construct(this->make_weak_ptr<HashTab>(), String("SHA1:"), Crypto::Hash::HashKind::SHA1);
        m_hash_widgets.append({ sha1_widget });
        insert_child_before(sha1_widget, *spacer);
    }
    {
        auto sha256_widget = HashWidget::construct(this->make_weak_ptr<HashTab>(), String("SHA256:"), Crypto::Hash::HashKind::SHA256);
        m_hash_widgets.append({ sha256_widget });
        insert_child_before(sha256_widget, *spacer);
    }
    {
        auto sha512_widget = HashWidget::construct(this->make_weak_ptr<HashTab>(), String("SHA512:"), Crypto::Hash::HashKind::SHA512);
        m_hash_widgets.append({ sha512_widget });
        insert_child_before(sha512_widget, *spacer);
    }

    m_calculate_button = find_descendant_of_type_named<GUI::Button>("calculate_button");
    VERIFY(!m_calculate_button.is_null());
    m_calculate_button->set_visible(!calculate_on_show);
    m_calculate_button->on_click = [this](auto) -> void {
        this->background_calculate_hashes();
    };
}

HashTab::~HashTab()
{
}

struct HashKindTraits {
    static inline bool equals(Crypto::Hash::HashKind a, Crypto::Hash::HashKind b)
    {
        return a == b;
    }

    static inline unsigned hash(Crypto::Hash::HashKind input)
    {
        return static_cast<unsigned>(input);
    }
};

void HashTab::background_calculate_hashes()
{
    m_calculate_button->set_enabled(false);
    Threading::BackgroundAction<Optional<HashMap<Crypto::Hash::HashKind, String, HashKindTraits, true>>>::create(
        [this](auto& action) -> Optional<HashMap<Crypto::Hash::HashKind, String, HashKindTraits, true>> {
            auto file_to_hash = Core::File::construct(m_path);
            if (!file_to_hash->open(Core::OpenMode::ReadOnly)) {
                action.cancel();
                dbgln("Opening file for hashing failed.");
                return {};
            }

            Vector<Crypto::Hash::Manager, NUMBER_OF_HASH_TYPES> hash_managers = {};
            for (auto& hash_widget : m_hash_widgets) {
                hash_managers.append(Crypto::Hash::Manager(hash_widget->m_hash_kind));
                hash_widget->wait_for_result();
            }

            HashMap<Crypto::Hash::HashKind, String, HashKindTraits, true> result_map = {};
            result_map.ensure_capacity(m_hash_widgets.size());

            size_t iteration = 0;
            while (!file_to_hash->eof() && !file_to_hash->has_error()) {
                auto buffer = file_to_hash->read(PAGE_SIZE);
                for (auto& hash_manager : hash_managers)
                    hash_manager.update(buffer);
                if (++iteration % 8 == 0)
                    sched_yield();
            }
            file_to_hash->close();

            for (auto& hash_manager : hash_managers) {
                auto digest = hash_manager.digest();
                auto digest_data = digest.immutable_data();
                StringBuilder hash_string_builder;
                for (size_t i = 0; i < hash_manager.digest_size(); i++)
                    hash_string_builder.appendff("{:02x}", digest_data[i]);
                result_map.set(hash_manager.kind(), hash_string_builder.build());
            }

            return { result_map };
        },
        [this](Optional<HashMap<Crypto::Hash::HashKind, String, HashKindTraits, true>> results) -> void {
            if (results.has_value()) {
                update_clipboard();
                for (auto& hash_widget : m_hash_widgets) {
                    hash_widget->set_hash(results.value().get(hash_widget->m_hash_kind));
                    hash_widget->update_status(make_weak_ptr<HashTab>());
                }
            } else {
                for (auto& hash_widget : m_hash_widgets) {
                    hash_widget->set_hash({});
                    hash_widget->update_status(make_weak_ptr<HashTab>());
                }
                m_calculate_button->set_enabled(true);
            }
        });
}

void HashTab::show_event(GUI::ShowEvent&)
{
    if (m_calculate_on_show)
        background_calculate_hashes();
}

void HashTab::clipboard_content_did_change(String const& mime_type)
{
    if (mime_type.starts_with("text/")) {
        update_clipboard();
        for (auto& hash_widget : m_hash_widgets)
            hash_widget->update_status(make_weak_ptr<HashTab>());
    }
}

void HashTab::update_clipboard()
{
    auto const& clipboard_data_and_type = GUI::Clipboard::the().data_and_type();
    if (clipboard_data_and_type.mime_type.starts_with("text/"))
        m_clipboard = StringView(clipboard_data_and_type.data).to_string();
    else
        m_clipboard = {};
}

HashTab::HashWidget::HashWidget(WeakPtr<HashTab> containing_tab, String const label, Crypto::Hash::HashKind hash_kind)
    : m_hash_kind(hash_kind)
{
    VERIFY(!containing_tab.is_null());
    load_from_gml(properties_window_hashes_hash_widget_gml);

    m_hash_name_label = find_descendant_of_type_named<GUI::Label>("hash_kind_label");
    VERIFY(!m_hash_name_label.is_null());
    m_hash_name_label->set_text(label);

    m_status_icon = find_descendant_of_type_named<GUI::ImageWidget>("status_icon");
    VERIFY(!m_status_icon.is_null());
    m_status_icon->set_bitmap(RefPtr(containing_tab->m_icon_hash_load));

    m_hash_result_box = find_descendant_of_type_named<GUI::TextBox>("hash_text");
    VERIFY(!m_hash_result_box.is_null());

    m_copy_button = find_descendant_of_type_named<GUI::Button>("copy_button");
    VERIFY(!m_copy_button.is_null());
    m_copy_button->set_icon(RefPtr(containing_tab->m_icon_copy));
    m_copy_button->on_click = [this](auto) -> void {
        this->put_on_clipboard();
    };
}

HashTab::HashWidget::~HashWidget()
{
}

void HashTab::HashWidget::wait_for_result()
{
    m_hash_result_box->set_enabled(false);
    m_hash_result_box->set_placeholder("calculating hash");
    m_hash_result_box->set_text("");
}

void HashTab::HashWidget::set_hash(Optional<String> new_hash_value)
{
    m_hash_result = new_hash_value;
    if (m_hash_result.has_value()) {
        m_hash_result_box->set_text(m_hash_result.value());
        m_hash_result_box->set_cursor_and_focus_line(0, 0);
        m_hash_result_box->set_enabled(true);
        m_copy_button->set_enabled(true);
    } else {
        m_hash_result_box->set_text("");
        m_hash_result_box->set_placeholder("hash error");
        m_hash_result_box->set_enabled(false);
        m_copy_button->set_enabled(false);
    }
}

void HashTab::HashWidget::put_on_clipboard() const
{
    if (m_hash_result.has_value())
        GUI::Clipboard::the().set_plain_text(m_hash_result.value());
}

void HashTab::HashWidget::update_status(WeakPtr<HashTab> containing_tab)
{
    VERIFY(!containing_tab.is_null());
    if (m_hash_result.has_value()) {
        if (containing_tab->m_clipboard.has_value()) {
            if (containing_tab->m_clipboard.value().contains(m_hash_result.value(), CaseSensitivity::CaseInsensitive)) {
                m_status_icon->set_bitmap(RefPtr(containing_tab->m_icon_hash_matching));
                m_status_icon->set_tooltip("hash status compared to clipboard:\nmatching clipboard");
            } else {
                m_status_icon->set_bitmap(RefPtr(containing_tab->m_icon_hash_not_matching));
                m_status_icon->set_tooltip("hash status compared to clipboard:\nnot matching clipboard");
            }
        } else {
            m_status_icon->set_bitmap(RefPtr(containing_tab->m_icon_hash));
            m_status_icon->set_tooltip("hash status compared to clipboard:\nempty clipboard");
        }
    } else {
        m_status_icon->set_bitmap(RefPtr(containing_tab->m_icon_hash_load));
        m_status_icon->set_tooltip("hash status compared to clipboard:\nhash unknown");
        m_hash_result_box->set_enabled(false);
        m_copy_button->set_enabled(false);
    }
}
