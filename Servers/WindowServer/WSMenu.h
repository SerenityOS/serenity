/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/String.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/WeakPtr.h>
#include <LibCore/CObject.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSMenuItem.h>
#include <WindowServer/WSWindow.h>

class WSClientConnection;
class WSMenuBar;
class WSEvent;
class Font;

class WSMenu final : public CObject {
    C_OBJECT(WSMenu)
public:
    WSMenu(WSClientConnection*, int menu_id, const String& name);
    virtual ~WSMenu() override;

    WSClientConnection* client() { return m_client; }
    const WSClientConnection* client() const { return m_client; }
    int menu_id() const { return m_menu_id; }

    WSMenuBar* menubar() { return m_menubar; }
    const WSMenuBar* menubar() const { return m_menubar; }
    void set_menubar(WSMenuBar* menubar) { m_menubar = menubar; }

    bool is_empty() const { return m_items.is_empty(); }
    int item_count() const { return m_items.size(); }
    const WSMenuItem& item(int index) const { return m_items.at(index); }
    WSMenuItem& item(int index) { return m_items.at(index); }

    void add_item(NonnullOwnPtr<WSMenuItem>&& item) { m_items.append(move(item)); }

    String name() const { return m_name; }

    template<typename Callback>
    void for_each_item(Callback callback) const
    {
        for (auto& item : m_items)
            callback(item);
    }

    Rect text_rect_in_menubar() const { return m_text_rect_in_menubar; }
    void set_text_rect_in_menubar(const Rect& rect) { m_text_rect_in_menubar = rect; }

    Rect rect_in_menubar() const { return m_rect_in_menubar; }
    void set_rect_in_menubar(const Rect& rect) { m_rect_in_menubar = rect; }

    WSWindow* menu_window() { return m_menu_window.ptr(); }
    WSWindow& ensure_menu_window();

    WSWindow* window_menu_of() { return m_window_menu_of; }
    void set_window_menu_of(WSWindow& window) { m_window_menu_of = window.make_weak_ptr(); }
    bool is_window_menu_open() { return m_is_window_menu_open; }
    void set_window_menu_open(bool is_open) { m_is_window_menu_open = is_open; }

    int width() const;
    int height() const;

    int item_height() const { return 20; }
    int frame_thickness() const { return 3; }
    int horizontal_padding() const { return left_padding() + right_padding(); }
    int left_padding() const { return 14; }
    int right_padding() const { return 14; }

    void draw();
    const Font& font() const;

    WSMenuItem* item_with_identifier(unsigned);
    void redraw();

    WSMenuItem* hovered_item() const;
    void clear_hovered_item();

    Function<void(WSMenuItem&)> on_item_activation;

    void close();

    void popup(const Point&, bool is_submenu = false);

    bool is_menu_ancestor_of(const WSMenu&) const;

    void redraw_if_theme_changed();

private:
    virtual void event(CEvent&) override;

    int item_index_at(const Point&);
    int padding_between_text_and_shortcut() const { return 50; }
    void did_activate(WSMenuItem&);
    void open_hovered_item();
    void update_for_new_hovered_item();
    void decend_into_submenu_at_hovered_item();

    WSClientConnection* m_client { nullptr };
    int m_menu_id { 0 };
    String m_name;
    Rect m_rect_in_menubar;
    Rect m_text_rect_in_menubar;
    WSMenuBar* m_menubar { nullptr };
    NonnullOwnPtrVector<WSMenuItem> m_items;
    RefPtr<WSWindow> m_menu_window;

    WeakPtr<WSWindow> m_window_menu_of;
    bool m_is_window_menu_open = { false };
    Point m_last_position_in_hover;
    int m_theme_index_at_last_paint { -1 };
    int m_hovered_item_index { -1 };
    bool m_in_submenu { false };
};
