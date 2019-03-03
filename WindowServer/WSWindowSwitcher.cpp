#include <WindowServer/WSWindowSwitcher.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSMessage.h>
#include <SharedGraphics/Font.h>

WSWindowSwitcher::WSWindowSwitcher()
{
}

WSWindowSwitcher::~WSWindowSwitcher()
{
}

void WSWindowSwitcher::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    if (!m_visible) {
        WSWindowManager::the().invalidate(m_rect);
        return;
    }
    invalidate();
}

WSWindow* WSWindowSwitcher::selected_window()
{
    if (m_selected_index < 0 || m_selected_index >= m_windows.size())
        return nullptr;
    return m_windows[m_selected_index].ptr();
}

void WSWindowSwitcher::on_key_event(const WSKeyEvent& event)
{
    if (event.type() == WSMessage::KeyUp) {
        if (event.key() == Key_Logo) {
            if (auto* window = selected_window()) {
                WSWindowManager::the().set_active_window(window);
                WSWindowManager::the().move_to_front(*window);
            }
            WSWindowManager::the().set_highlight_window(nullptr);
            hide();
        }
        return;
    }
    if (event.key() != Key_Tab) {
        hide();
        return;
    }
    ASSERT(!m_windows.is_empty());
    m_selected_index = (m_selected_index + 1) % m_windows.size();
    ASSERT(m_selected_index < m_windows.size());
    auto* highlight_window = m_windows.at(m_selected_index).ptr();
    ASSERT(highlight_window);
    WSWindowManager::the().set_highlight_window(highlight_window);
    WSWindowManager::the().invalidate(m_rect);
}

void WSWindowSwitcher::draw(Painter& painter)
{
    painter.translate(m_rect.location());
    painter.fill_rect({ { }, m_rect.size() }, Color::LightGray);
    painter.draw_rect({ { }, m_rect.size() }, Color::DarkGray);

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
            painter.fill_rect(item_rect, Color::from_rgb(0x84351a));
            text_color = Color::White;
            rect_text_color = Color::LightGray;
        } else {
            text_color = Color::Black;
            rect_text_color = Color::DarkGray;
        }

        painter.set_font(Font::default_bold_font());
        painter.draw_text(item_rect, window.title(), TextAlignment::CenterLeft, text_color);
        painter.set_font(WSWindowManager::the().font());
        painter.draw_text(item_rect, window.rect().to_string(), TextAlignment::CenterRight, rect_text_color);
    }
    painter.translate(-m_rect.x(), -m_rect.y());
}

void WSWindowSwitcher::invalidate()
{
    WSWindow* selected_window = nullptr;
    if (m_selected_index > 0 && m_windows[m_selected_index])
        selected_window = m_windows[m_selected_index].ptr();
    m_windows.clear();
    m_selected_index = 0;
    int window_count = 0;
    int longest_title = 0;
    WSWindowManager::the().for_each_visible_window_of_type_from_back_to_front(WSWindowType::Normal, [&] (WSWindow& window) {
        ++window_count;
        longest_title = max(longest_title, window.title().length());
        if (selected_window == &window)
            m_selected_index = m_windows.size();
        m_windows.append(window.make_weak_ptr());
        return IterationDecision::Continue;
    });
    if (m_windows.is_empty()) {
        hide();
        return;
    }
    int space_for_window_rect = WSWindowManager::the().font().glyph_width() * 24;
    m_rect.set_width(longest_title * WSWindowManager::the().font().glyph_width() + space_for_window_rect + padding() * 2);
    m_rect.set_height(window_count * item_height() + padding() * 2);
    m_rect.center_within(WSWindowManager::the().m_screen_rect);
    WSWindowManager::the().invalidate(m_rect);
}
