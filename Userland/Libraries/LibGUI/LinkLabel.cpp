/*
 * Copyright (c) 2020, Alex McGrath <amk@amk.ie>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Event.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, LinkLabel)

namespace GUI {

ErrorOr<NonnullRefPtr<LinkLabel>> LinkLabel::try_create(String text)
{
    auto label = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) LinkLabel(move(text))));
    TRY(label->create_actions());
    label->create_menus();
    return label;
}

LinkLabel::LinkLabel(String text)
    : Label(move(text))
{
    set_foreground_role(Gfx::ColorRole::Link);
    set_focus_policy(FocusPolicy::TabFocus);
}

ErrorOr<void> LinkLabel::create_actions()
{
    m_open_action = GUI::Action::create(
        "Show in File Manager", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"sv)), [&](const GUI::Action&) {
            if (on_click)
                on_click();
        },
        this);

    m_copy_action = CommonActions::make_copy_action([this](auto&) { Clipboard::the().set_plain_text(text()); }, this);
    return {};
}

void LinkLabel::create_menus()
{
    m_context_menu = Menu::construct();
    m_context_menu->add_action(*m_open_action);
    m_context_menu->add_separator();
    m_context_menu->add_action(*m_copy_action);
}

void LinkLabel::set_hovered(bool hover)
{
    if (hover == m_hovered)
        return;

    m_hovered = hover;
    set_override_cursor(hover ? Gfx::StandardCursor::Hand : Gfx::StandardCursor::None);
    update();
}

void LinkLabel::mousemove_event(MouseEvent& event)
{
    constexpr int extra_target_width = 3;
    set_hovered(event.position().x() <= font().width(text()) + extra_target_width);
}

void LinkLabel::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    Label::mousedown_event(event);
    if (m_hovered && on_click) {
        on_click();
    }
}

void LinkLabel::keydown_event(KeyEvent& event)
{
    Label::keydown_event(event);
    if (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Space) {
        if (on_click)
            on_click();
    }
}

void LinkLabel::paint_event(PaintEvent& event)
{
    Label::paint_event(event);
    GUI::Painter painter(*this);

    if (m_hovered)
        painter.draw_line({ 0, rect().bottom() - 1 }, { font().width_rounded_up(text()), rect().bottom() - 1 }, palette().link());

    if (is_focused())
        painter.draw_focus_rect(text_rect(), palette().focus_outline());
}

void LinkLabel::leave_event(Core::Event& event)
{
    Label::leave_event(event);
    set_hovered(false);
}

void LinkLabel::did_change_text()
{
    Label::did_change_text();
    update_tooltip_if_needed();
}

void LinkLabel::update_tooltip_if_needed()
{
    if (width() < font().width(text())) {
        set_tooltip(text());
    } else {
        set_tooltip({});
    }
}

void LinkLabel::resize_event(ResizeEvent& event)
{
    Label::resize_event(event);
    update_tooltip_if_needed();
}

void LinkLabel::context_menu_event(ContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position(), m_open_action);
}

}
