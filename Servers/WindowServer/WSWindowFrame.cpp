#include <WindowServer/WSWindowFrame.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSMessage.h>
#include <WindowServer/WSButton.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/Font.h>
#include <SharedGraphics/StylePainter.h>

static const char* s_close_button_bitmap_data = {
    "##    ##"
    "###  ###"
    " ###### "
    "  ####  "
    "   ##   "
    "  ####  "
    " ###### "
    "###  ###"
    "##    ##"
};

static CharacterBitmap* s_close_button_bitmap;
static const int s_close_button_bitmap_width = 8;
static const int s_close_button_bitmap_height = 9;

WSWindowFrame::WSWindowFrame(WSWindow& window)
    : m_window(window)
{
    if (!s_close_button_bitmap)
        s_close_button_bitmap = &CharacterBitmap::create_from_ascii(s_close_button_bitmap_data, s_close_button_bitmap_width, s_close_button_bitmap_height).leak_ref();

    m_buttons.append(make<WSButton>(*s_close_button_bitmap, [this] {
        m_window.on_message(WSMessage(WSMessage::WindowCloseRequest));
    }));
}

WSWindowFrame::~WSWindowFrame()
{
}

static const int window_titlebar_height = 18;

static inline Rect menu_window_rect(const Rect& rect)
{
    return rect.inflated(2, 2);
}

static inline Rect title_bar_rect(const Rect& window)
{
    return {
        window.x() - 1,
        window.y() - window_titlebar_height,
        window.width() + 2,
        window_titlebar_height
    };
}

static inline Rect title_bar_icon_rect(const Rect& window)
{
    auto titlebar_rect = title_bar_rect(window);
    return {
        titlebar_rect.x() + 2,
        titlebar_rect.y(),
        16,
        titlebar_rect.height(),
    };
}

static inline Rect title_bar_text_rect(const Rect& window)
{
    auto titlebar_rect = title_bar_rect(window);
    auto titlebar_icon_rect = title_bar_icon_rect(window);
    return {
        titlebar_rect.x() + 2 + titlebar_icon_rect.width() + 2,
        titlebar_rect.y(),
        titlebar_rect.width() - 4 - titlebar_icon_rect.width() - 2,
        titlebar_rect.height()
    };
}

static inline Rect border_window_rect(const Rect& window)
{
    auto titlebar_rect = title_bar_rect(window);
    return { titlebar_rect.x() - 1,
        titlebar_rect.y() - 1,
        titlebar_rect.width() + 2,
        window_titlebar_height + window.height() + 3
    };
}

static inline Rect outer_window_rect(const Rect& window)
{
    auto rect = border_window_rect(window);
    rect.inflate(2, 2);
    return rect;
}

static inline Rect outer_window_rect(const WSWindow& window)
{
    if (window.type() == WSWindowType::Menu)
        return menu_window_rect(window.rect());
    if (window.type() == WSWindowType::WindowSwitcher)
        return window.rect();
    if (window.type() == WSWindowType::Taskbar)
        return window.rect();
    ASSERT(window.type() == WSWindowType::Normal);
    return outer_window_rect(window.rect());
}

void WSWindowFrame::paint(Painter& painter)
{
    //printf("[WM] paint_window_frame {%p}, rect: %d,%d %dx%d\n", &window, window.rect().x(), window.rect().y(), window.rect().width(), window.rect().height());

    if (m_window.type() == WSWindowType::Menu) {
        painter.draw_rect(menu_window_rect(m_window.rect()), Color::LightGray);
        return;
    }

    if (m_window.type() == WSWindowType::WindowSwitcher)
        return;

    if (m_window.type() == WSWindowType::Taskbar)
        return;

    auto& window = m_window;

    auto titlebar_rect = title_bar_rect(window.rect());
    auto titlebar_icon_rect = title_bar_icon_rect(window.rect());
    auto titlebar_inner_rect = title_bar_text_rect(window.rect());
    auto outer_rect = outer_window_rect(window);
    auto border_rect = border_window_rect(window.rect());

    auto titlebar_title_rect = titlebar_inner_rect;
    titlebar_title_rect.set_width(Font::default_bold_font().width(window.title()));

    Rect inner_border_rect {
        window.x() - 1,
        window.y() - 1,
        window.width() + 2,
        window.height() + 2
    };

    Color title_color;
    Color border_color;
    Color border_color2;
    Color middle_border_color;

    auto& wm = WSWindowManager::the();

    if (&window == wm.m_highlight_window.ptr()) {
        border_color = wm.m_highlight_window_border_color;
        border_color2 = wm.m_highlight_window_border_color2;
        title_color = wm.m_highlight_window_title_color;
        middle_border_color = Color::White;
    } else if (&window == wm.m_drag_window.ptr()) {
        border_color = wm.m_dragging_window_border_color;
        border_color2 = wm.m_dragging_window_border_color2;
        title_color = wm.m_dragging_window_title_color;
        middle_border_color = Color::from_rgb(0xf9b36a);
    } else if (&window == wm.m_active_window.ptr()) {
        border_color = wm.m_active_window_border_color;
        border_color2 = wm.m_active_window_border_color2;
        title_color = wm.m_active_window_title_color;
        middle_border_color = Color::from_rgb(0x8f673d);
    } else {
        border_color = wm.m_inactive_window_border_color;
        border_color2 = wm.m_inactive_window_border_color2;
        title_color = wm.m_inactive_window_title_color;
        middle_border_color = Color::MidGray;
    }

    auto leftmost_button_rect = m_buttons.is_empty() ? Rect() : m_buttons.last()->rect();

    painter.fill_rect_with_gradient(titlebar_rect, border_color, border_color2);
    for (int i = 2; i <= titlebar_inner_rect.height() - 4; i += 2) {
        painter.draw_line({ titlebar_title_rect.right() + 4, titlebar_inner_rect.y() + i }, { leftmost_button_rect.left() - 3, titlebar_inner_rect.y() + i }, border_color);
    }
    painter.draw_rect(border_rect, middle_border_color);
    painter.draw_rect(outer_rect, border_color);
    painter.draw_rect(inner_border_rect, border_color);

    painter.draw_text(titlebar_title_rect, window.title(), wm.window_title_font(), TextAlignment::CenterLeft, title_color);

    painter.blit(titlebar_icon_rect.location(), window.icon(), window.icon().rect());

    for (auto& button : m_buttons) {
        button->paint(painter);
    }
}

Rect WSWindowFrame::rect() const
{
    if (m_window.type() == WSWindowType::Menu)
        return menu_window_rect(m_window.rect());
    if (m_window.type() == WSWindowType::Normal)
        return outer_window_rect(m_window);
    if (m_window.type() == WSWindowType::WindowSwitcher)
        return m_window.rect();
    if (m_window.type() == WSWindowType::Taskbar)
        return m_window.rect();
    ASSERT_NOT_REACHED();
}

void WSWindowFrame::notify_window_rect_changed(const Rect& old_rect, const Rect& new_rect)
{
    int window_button_width = 15;
    int window_button_height = 15;
    int x = title_bar_text_rect(new_rect).right() + 1;;
    for (auto& button : m_buttons) {
        x -= window_button_width;
        Rect rect { x, 0, window_button_width, window_button_height };
        rect.center_vertically_within(title_bar_rect(new_rect));
        rect.set_y(rect.y() - 1);
        button->set_rect(rect);
    }

    auto& wm = WSWindowManager::the();
    wm.invalidate(outer_window_rect(old_rect));
    wm.invalidate(outer_window_rect(new_rect));
    wm.notify_rect_changed(m_window, old_rect, new_rect);
}

void WSWindowFrame::on_mouse_event(const WSMouseEvent& event)
{
    auto& wm = WSWindowManager::the();
    if (m_window.type() != WSWindowType::Normal)
        return;
    if (title_bar_rect(m_window.rect()).contains(event.position())) {
        if (event.type() == WSMessage::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        for (auto& button : m_buttons) {
            if (button->rect().contains(event.position()))
                return button->on_mouse_event(event);
        }
        if (event.type() == WSMessage::MouseDown && event.button() == MouseButton::Left)
            wm.start_window_drag(m_window, event);
    }
}
