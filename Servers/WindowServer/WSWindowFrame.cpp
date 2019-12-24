#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/Font.h>
#include <LibDraw/Painter.h>
#include <LibDraw/StylePainter.h>
#include <WindowServer/WSButton.h>
#include <WindowServer/WSCompositor.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowFrame.h>
#include <WindowServer/WSWindowManager.h>

static const int window_titlebar_height = 19;

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

static const char* s_minimize_button_bitmap_data = {
    "        "
    "        "
    "        "
    " ###### "
    "  ####  "
    "   ##   "
    "        "
    "        "
    "        "
};

static CharacterBitmap* s_minimize_button_bitmap;
static const int s_minimize_button_bitmap_width = 8;
static const int s_minimize_button_bitmap_height = 9;

static const char* s_maximize_button_bitmap_data = {
    "        "
    "        "
    "        "
    "   ##   "
    "  ####  "
    " ###### "
    "        "
    "        "
    "        "
};

static CharacterBitmap* s_maximize_button_bitmap;
static const int s_maximize_button_bitmap_width = 8;
static const int s_maximize_button_bitmap_height = 9;

static const char* s_unmaximize_button_bitmap_data = {
    "        "
    "   ##   "
    "  ####  "
    " ###### "
    "        "
    " ###### "
    "  ####  "
    "   ##   "
    "        "
};

static CharacterBitmap* s_unmaximize_button_bitmap;
static const int s_unmaximize_button_bitmap_width = 8;
static const int s_unmaximize_button_bitmap_height = 9;

WSWindowFrame::WSWindowFrame(WSWindow& window)
    : m_window(window)
{
    if (!s_close_button_bitmap)
        s_close_button_bitmap = &CharacterBitmap::create_from_ascii(s_close_button_bitmap_data, s_close_button_bitmap_width, s_close_button_bitmap_height).leak_ref();

    if (!s_minimize_button_bitmap)
        s_minimize_button_bitmap = &CharacterBitmap::create_from_ascii(s_minimize_button_bitmap_data, s_minimize_button_bitmap_width, s_minimize_button_bitmap_height).leak_ref();

    if (!s_maximize_button_bitmap)
        s_maximize_button_bitmap = &CharacterBitmap::create_from_ascii(s_maximize_button_bitmap_data, s_maximize_button_bitmap_width, s_maximize_button_bitmap_height).leak_ref();

    if (!s_unmaximize_button_bitmap)
        s_unmaximize_button_bitmap = &CharacterBitmap::create_from_ascii(s_unmaximize_button_bitmap_data, s_unmaximize_button_bitmap_width, s_unmaximize_button_bitmap_height).leak_ref();

    m_buttons.append(make<WSButton>(*this, *s_close_button_bitmap, [this](auto&) {
        m_window.request_close();
    }));

    if (window.is_resizable()) {
        auto button = make<WSButton>(*this, *s_maximize_button_bitmap, [this](auto&) {
            m_window.set_maximized(!m_window.is_maximized());
        });
        m_maximize_button = button.ptr();
        m_buttons.append(move(button));
    }

    m_buttons.append(make<WSButton>(*this, *s_minimize_button_bitmap, [this](auto&) {
        m_window.set_minimized(true);
    }));
}

WSWindowFrame::~WSWindowFrame()
{
}

void WSWindowFrame::did_set_maximized(Badge<WSWindow>, bool maximized)
{
    ASSERT(m_maximize_button);
    m_maximize_button->set_bitmap(maximized ? *s_unmaximize_button_bitmap : *s_maximize_button_bitmap);
}

Rect WSWindowFrame::title_bar_rect() const
{
    return { 3, 3, m_window.width(), window_titlebar_height };
}

Rect WSWindowFrame::title_bar_icon_rect() const
{
    auto titlebar_rect = title_bar_rect();
    return {
        titlebar_rect.x() + 1,
        titlebar_rect.y() + 2,
        16,
        titlebar_rect.height(),
    };
}

Rect WSWindowFrame::title_bar_text_rect() const
{
    auto titlebar_rect = title_bar_rect();
    auto titlebar_icon_rect = title_bar_icon_rect();
    return {
        titlebar_rect.x() + 2 + titlebar_icon_rect.width() + 2,
        titlebar_rect.y(),
        titlebar_rect.width() - 4 - titlebar_icon_rect.width() - 2,
        titlebar_rect.height()
    };
}

void WSWindowFrame::paint(Painter& painter)
{
    PainterStateSaver saver(painter);
    painter.translate(rect().location());

    if (m_window.type() != WSWindowType::Normal)
        return;

    auto& palette = WSWindowManager::the().palette();
    auto& window = m_window;

    auto titlebar_rect = title_bar_rect();
    auto titlebar_icon_rect = title_bar_icon_rect();
    auto titlebar_inner_rect = title_bar_text_rect();
    Rect outer_rect = { {}, rect().size() };

    auto titlebar_title_rect = titlebar_inner_rect;
    titlebar_title_rect.set_width(Font::default_bold_font().width(window.title()));

    Color title_color;
    Color border_color;
    Color border_color2;

    auto& wm = WSWindowManager::the();

    if (&window == wm.m_highlight_window) {
        border_color = palette.highlight_window_border1();
        border_color2 = palette.highlight_window_border2();
        title_color = palette.highlight_window_title();
    } else if (&window == wm.m_move_window) {
        border_color = palette.moving_window_border1();
        border_color2 = palette.moving_window_border2();
        title_color = palette.moving_window_title();
    } else if (&window == wm.m_active_window) {
        border_color = palette.active_window_border1();
        border_color2 = palette.active_window_border2();
        title_color = palette.active_window_title();
    } else {
        border_color = palette.inactive_window_border1();
        border_color2 = palette.inactive_window_border2();
        title_color = palette.inactive_window_title();
    }

    StylePainter::paint_window_frame(painter, outer_rect, palette);

    if (!window.show_titlebar())
        return;

    painter.draw_line(titlebar_rect.bottom_left().translated(0, 1), titlebar_rect.bottom_right().translated(0, 1), palette.button());

    auto leftmost_button_rect = m_buttons.is_empty() ? Rect() : m_buttons.last().relative_rect();

    painter.fill_rect_with_gradient(titlebar_rect, border_color, border_color2);

    int stripe_left = titlebar_title_rect.right() + 4;
    int stripe_right = leftmost_button_rect.left() - 3;
    if (stripe_left && stripe_right && stripe_left < stripe_right) {
        for (int i = 2; i <= titlebar_inner_rect.height() - 2; i += 2) {
            painter.draw_line({ stripe_left, titlebar_inner_rect.y() + i }, { stripe_right, titlebar_inner_rect.y() + i }, border_color);
        }
    }

    auto clipped_title_rect = titlebar_title_rect;
    clipped_title_rect.set_width(stripe_right - clipped_title_rect.x());
    if (!clipped_title_rect.is_empty()) {
        painter.draw_text(clipped_title_rect.translated(1, 2), window.title(), wm.window_title_font(), TextAlignment::CenterLeft, border_color.darkened(0.4), TextElision::Right);
        // FIXME: The translated(0, 1) wouldn't be necessary if we could center text based on its baseline.
        painter.draw_text(clipped_title_rect.translated(0, 1), window.title(), wm.window_title_font(), TextAlignment::CenterLeft, title_color, TextElision::Right);
    }

    painter.blit(titlebar_icon_rect.location(), window.icon(), window.icon().rect());

    for (auto& button : m_buttons) {
        button.paint(painter);
    }
}

static Rect frame_rect_for_window(WSWindow& window, const Rect& rect)
{
    auto type = window.type();
    auto offset = !window.show_titlebar() ? (window_titlebar_height + 1) : 0;

    switch (type) {
    case WSWindowType::Normal:
        return { rect.x() - 3,
            rect.y() - window_titlebar_height - 4 + offset,
            rect.width() + 6,
            rect.height() + 7 + window_titlebar_height - offset };
    default:
        return rect;
    }
}

static Rect frame_rect_for_window(WSWindow& window)
{
    return frame_rect_for_window(window, window.rect());
}

Rect WSWindowFrame::rect() const
{
    return frame_rect_for_window(m_window);
}

void WSWindowFrame::invalidate_title_bar()
{
    WSWindowManager::the().invalidate(title_bar_rect().translated(rect().location()));
}

void WSWindowFrame::notify_window_rect_changed(const Rect& old_rect, const Rect& new_rect)
{
    int window_button_width = 15;
    int window_button_height = 15;
    int x = title_bar_text_rect().right() + 1;

    for (auto& button : m_buttons) {
        x -= window_button_width;
        Rect rect { x, 0, window_button_width, window_button_height };
        rect.center_vertically_within(title_bar_text_rect());
        button.set_relative_rect(rect);
    }

    auto& wm = WSWindowManager::the();
    wm.invalidate(frame_rect_for_window(m_window, old_rect));
    wm.invalidate(frame_rect_for_window(m_window, new_rect));
    wm.notify_rect_changed(m_window, old_rect, new_rect);
}

void WSWindowFrame::on_mouse_event(const WSMouseEvent& event)
{
    ASSERT(!m_window.is_fullscreen());

    auto& wm = WSWindowManager::the();
    if (m_window.type() != WSWindowType::Normal)
        return;

    if (event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left && title_bar_icon_rect().contains(event.position())) {
        m_window.popup_window_menu(event.position().translated(rect().location()));
        return;
    }

    // This is slightly hackish, but expand the title bar rect by one pixel downwards,
    // so that mouse events between the title bar and window contents don't act like
    // mouse events on the border.
    auto adjusted_title_bar_rect = title_bar_rect();
    adjusted_title_bar_rect.set_height(adjusted_title_bar_rect.height() + 1);

    if (adjusted_title_bar_rect.contains(event.position())) {
        wm.clear_resize_candidate();

        if (event.type() == WSEvent::MouseDown)
            wm.move_to_front_and_make_active(m_window);

        for (auto& button : m_buttons) {
            if (button.relative_rect().contains(event.position()))
                return button.on_mouse_event(event.translated(-button.relative_rect().location()));
        }
        if (event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left)
            wm.start_window_move(m_window, event.translated(rect().location()));
        return;
    }

    if (m_window.is_resizable() && event.type() == WSEvent::MouseMove && event.buttons() == 0) {
        constexpr ResizeDirection direction_for_hot_area[3][3] = {
            { ResizeDirection::UpLeft, ResizeDirection::Up, ResizeDirection::UpRight },
            { ResizeDirection::Left, ResizeDirection::None, ResizeDirection::Right },
            { ResizeDirection::DownLeft, ResizeDirection::Down, ResizeDirection::DownRight },
        };
        Rect outer_rect = { {}, rect().size() };
        ASSERT(outer_rect.contains(event.position()));
        int window_relative_x = event.x() - outer_rect.x();
        int window_relative_y = event.y() - outer_rect.y();
        int hot_area_row = min(2, window_relative_y / (outer_rect.height() / 3));
        int hot_area_column = min(2, window_relative_x / (outer_rect.width() / 3));
        wm.set_resize_candidate(m_window, direction_for_hot_area[hot_area_row][hot_area_column]);
        WSCompositor::the().invalidate_cursor();
        return;
    }

    if (m_window.is_resizable() && event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left)
        wm.start_window_resize(m_window, event.translated(rect().location()));
}
