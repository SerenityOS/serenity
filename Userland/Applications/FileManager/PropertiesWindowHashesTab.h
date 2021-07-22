/*
 * Copyright (c) 2021, FrHun <frhun@t-online.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/File.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibGUI/Button.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Event.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibThreading/BackgroundAction.h>

class HashTab final : public GUI::Widget
    , protected GUI::Clipboard::ClipboardClient {
    C_OBJECT(HashTab);
    AK_MAKE_NONCOPYABLE(HashTab);
    AK_MAKE_NONMOVABLE(HashTab);

public:
    virtual ~HashTab() override;

private:
    class HashWidget;
    constexpr static const unsigned NUMBER_OF_HASH_TYPES = 4;

    const String m_path;
    const bool m_calculate_on_show;

    RefPtr<Gfx::Bitmap> m_icon_hash_not_matching;
    RefPtr<Gfx::Bitmap> m_icon_hash_matching;
    RefPtr<Gfx::Bitmap> m_icon_hash_load;
    RefPtr<Gfx::Bitmap> m_icon_hash;
    RefPtr<Gfx::Bitmap> m_icon_copy;

    Optional<String> m_clipboard;

    RefPtr<GUI::Button> m_calculate_button;
    Vector<RefPtr<HashWidget>, NUMBER_OF_HASH_TYPES> m_hash_widgets;

    HashTab(String const& path, bool calculate_on_show);
    HashTab() = delete;

    virtual void show_event(GUI::ShowEvent&) override;
    virtual void clipboard_content_did_change(String const& mime_type) override;

    void background_calculate_hashes();
    void update_clipboard();

    class HashWidget final : public GUI::Widget {
        C_OBJECT(HashWidget);
        AK_MAKE_NONCOPYABLE(HashWidget);
        AK_MAKE_NONMOVABLE(HashWidget);

    public:
        const Crypto::Hash::HashKind m_hash_kind;
        virtual ~HashWidget() override;

        void put_on_clipboard() const;

        void wait_for_result();
        void set_hash(Optional<String>);
        void update_status(WeakPtr<HashTab> containing_tab);

    private:
        Optional<String> m_hash_result;
        RefPtr<GUI::Label> m_hash_name_label;
        RefPtr<GUI::ImageWidget> m_status_icon;
        RefPtr<GUI::TextBox> m_hash_result_box;
        RefPtr<GUI::Button> m_copy_button;

        HashWidget(WeakPtr<HashTab>, String const label, Crypto::Hash::HashKind);
        HashWidget() = delete;
    };
};
