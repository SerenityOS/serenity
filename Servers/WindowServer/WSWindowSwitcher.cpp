#include <LibDraw/Font.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/StylePainter.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSWindowSwitcher.h>

static WSWindowSwitcher* s_the;

WSWindowSwitcher& WSWindowSwitcher::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSWindowSwitcher::WSWindowSwitcher()
{
    s_the = this;
}

WSWindowSwitcher::~WSWindowSwitcher()
{
}

void WSWindowSwitcher::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    if (m_switcher_window)
        m_switcher_window->set_visible(visible);
    if (!m_visible)
        return;
    refresh();
}

WSWindow* WSWindowSwitcher::selected_window()
{
    if (m_selected_index < 0 || m_selected_index >= m_windows.size())
        return nullptr;
    return m_windows[m_selected_index].ptr();
}

void WSWindowSwitcher::on_key_event(const WSKeyEvent& event)
{
    if (event.type() == WSEvent::KeyUp) {
        if (event.key() == Key_Logo) {
            if (auto* window = selected_window())
                WSWindowManager::the().move_to_front_and_make_active(*window);
            WSWindowManager::the().set_highlight_window(nullptr);
            hide();
        }
        return;
    }
    if (event.key() != Key_Tab) {
        WSWindowManager::the().set_highlight_window(nullptr);
        hide();
        return;
    }
    ASSERT(!m_windows.is_empty());
    m_selected_index = (m_selected_index + 1) % m_windows.size();
    ASSERT(m_selected_index < m_windows.size());
    auto* highlight_window = m_windows.at(m_selected_index).ptr();
    ASSERT(highlight_window);
    WSWindowManager::the().set_highlight_window(highlight_window);
    draw();
    WSWindowManager::the().invalidate(m_rect);
}

void WSWindowSwitcher::draw()
{
    Painter painter(*m_switcher_window->backing_store());
    painter.fill_rect({ {}, m_rect.size() }, SystemColor::Window);
    painter.draw_rect({ {}, m_rect.size() }, SystemColor::ThreedShadow2);
    for (int index = 0; index < m_windows.size(); ++index) {
        auto& window = *m_windows.at(index);
        Rect item_rect {
            padding(),
            padding() + index * item_height(),
            m_rect.width() - padding() * 2,
            item_height()
        };
        Color text_color;
        Color rect_text_color;
        if (index == m_selected_index) {
            painter.fill_rect(item_rect, SystemColor::Selection);
            text_color = SystemColor::SelectionText;
            rect_text_color = SystemColor::ThreedShadow1;
        } else {
            text_color = SystemColor::WindowText;
            rect_text_color = SystemColor::ThreedShadow2;
        }
        item_rect.shrink(item_padding(), 0);
        Rect thumbnail_rect = { item_rect.location().translated(0, 5), { thumbnail_width(), thumbnail_height() } };
        if (window.backing_store()) {
            painter.draw_scaled_bitmap(thumbnail_rect, *window.backing_store(), window.backing_store()->rect());
            StylePainter::paint_frame(painter, thumbnail_rect.inflated(4, 4), FrameShape::Container, FrameShadow::Sunken, 2);
        }
        Rect icon_rect = { thumbnail_rect.bottom_right().translated(-window.icon().width(), -window.icon().height()), { window.icon().width(), window.icon().height() } };
        painter.fill_rect(icon_rect, SystemColor::Window);
        painter.blit(icon_rect.location(), window.icon(), window.icon().rect());
        painter.draw_text(item_rect.translated(thumbnail_width() + 12, 0), window.title(), WSWindowManager::the().window_title_font(), TextAlignment::CenterLeft, text_color);
        painter.draw_text(item_rect, window.rect().to_string(), TextAlignment::CenterRight, rect_text_color);
    }
}

void WSWindowSwitcher::refresh()
{
    auto& wm = WSWindowManager::the();
    WSWindow* selected_window = nullptr;
    if (m_selected_index > 0 && m_windows[m_selected_index])
        selected_window = m_windows[m_selected_index].ptr();
    if (!selected_window)
        selected_window = wm.highlight_window() ? wm.highlight_window() : wm.active_window();
    m_windows.clear();
    m_selected_index = 0;
    int window_count = 0;
    int longest_title_width = 0;
    wm.for_each_visible_window_of_type_from_front_to_back(WSWindowType::Normal, [&](WSWindow& window) {
        ++window_count;
        longest_title_width = max(longest_title_width, wm.font().width(window.title()));
        if (selected_window == &window)
            m_selected_index = m_windows.size();
        m_windows.append(window.make_weak_ptr());
        return IterationDecision::Continue;
    },
        true);
    if (m_windows.is_empty()) {
        hide();
        return;
    }
    int space_for_window_rect = 180;
    m_rect.set_width(thumbnail_width() + longest_title_width + space_for_window_rect + padding() * 2 + item_padding() * 2);
    m_rect.set_height(window_count * item_height() + padding() * 2);
    m_rect.center_within(WSScreen::the().rect());
    if (!m_switcher_window)
        m_switcher_window = WSWindow::construct(*this, WSWindowType::WindowSwitcher);
    m_switcher_window->set_rect(m_rect);
    draw();
}

void WSWindowSwitcher::refresh_if_needed()
{
    if (m_visible) {
        refresh();
        WSWindowManager::the().invalidate(m_rect);
    }
}
