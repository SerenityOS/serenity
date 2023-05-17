/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentView.h"
#include "StringUtils.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Ladybird/HelperProcess.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibGfx/SystemTheme.h>
#include <LibMain/Main.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWebView/WebContentClient.h>
#include <QApplication>
#include <QCursor>
#include <QGuiApplication>
#include <QIcon>
#include <QLineEdit>
#include <QMimeData>
#include <QMouseEvent>
#include <QPaintEvent>
#include <QPainter>
#include <QPalette>
#include <QScrollBar>
#include <QTextEdit>
#include <QTimer>
#include <QToolTip>

namespace Ladybird {

bool is_using_dark_system_theme(QWidget&);

WebContentView::WebContentView(StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling enable_callgrind_profiling, UseLagomNetworking use_lagom_networking)
    : m_use_lagom_networking(use_lagom_networking)
    , m_webdriver_content_ipc_path(webdriver_content_ipc_path)
{
    setMouseTracking(true);
    setAcceptDrops(true);

    setFocusPolicy(Qt::FocusPolicy::StrongFocus);

    m_device_pixel_ratio = devicePixelRatio();
    m_inverse_pixel_scaling_ratio = 1.0 / m_device_pixel_ratio;

    verticalScrollBar()->setSingleStep(24);
    horizontalScrollBar()->setSingleStep(24);

    QObject::connect(verticalScrollBar(), &QScrollBar::valueChanged, [this](int) {
        update_viewport_rect();
    });
    QObject::connect(horizontalScrollBar(), &QScrollBar::valueChanged, [this](int) {
        update_viewport_rect();
    });

    create_client(enable_callgrind_profiling);
}

WebContentView::~WebContentView() = default;

unsigned get_button_from_qt_event(QSinglePointEvent const& event)
{
    if (event.button() == Qt::MouseButton::LeftButton)
        return 1;
    if (event.button() == Qt::MouseButton::RightButton)
        return 2;
    if (event.button() == Qt::MouseButton::MiddleButton)
        return 4;
    if (event.button() == Qt::MouseButton::BackButton)
        return 8;
    if (event.buttons() == Qt::MouseButton::ForwardButton)
        return 16;
    return 0;
}

unsigned get_buttons_from_qt_event(QSinglePointEvent const& event)
{
    unsigned buttons = 0;
    if (event.buttons() & Qt::MouseButton::LeftButton)
        buttons |= 1;
    if (event.buttons() & Qt::MouseButton::RightButton)
        buttons |= 2;
    if (event.buttons() & Qt::MouseButton::MiddleButton)
        buttons |= 4;
    if (event.buttons() & Qt::MouseButton::BackButton)
        buttons |= 8;
    if (event.buttons() & Qt::MouseButton::ForwardButton)
        buttons |= 16;
    return buttons;
}

unsigned get_modifiers_from_qt_mouse_event(QSinglePointEvent const& event)
{
    unsigned modifiers = 0;
    if (event.modifiers() & Qt::Modifier::ALT)
        modifiers |= 1;
    if (event.modifiers() & Qt::Modifier::CTRL)
        modifiers |= 2;
    if (event.modifiers() & Qt::Modifier::SHIFT)
        modifiers |= 4;
    return modifiers;
}

unsigned get_modifiers_from_qt_keyboard_event(QKeyEvent const& event)
{
    auto modifiers = 0;
    if (event.modifiers().testFlag(Qt::AltModifier))
        modifiers |= KeyModifier::Mod_Alt;
    if (event.modifiers().testFlag(Qt::ControlModifier))
        modifiers |= KeyModifier::Mod_Ctrl;
    if (event.modifiers().testFlag(Qt::MetaModifier))
        modifiers |= KeyModifier::Mod_Super;
    if (event.modifiers().testFlag(Qt::ShiftModifier))
        modifiers |= KeyModifier::Mod_Shift;
    if (event.modifiers().testFlag(Qt::AltModifier))
        modifiers |= KeyModifier::Mod_AltGr;
    if (event.modifiers().testFlag(Qt::KeypadModifier))
        modifiers |= KeyModifier::Mod_Keypad;
    return modifiers;
}

KeyCode get_keycode_from_qt_keyboard_event(QKeyEvent const& event)
{
    struct Mapping {
        constexpr Mapping(Qt::Key q, KeyCode s)
            : qt_key(q)
            , serenity_key(s)
        {
        }

        Qt::Key qt_key;
        KeyCode serenity_key;
    };

    // https://doc.qt.io/qt-6/qt.html#Key-enum
    constexpr Mapping mappings[] = {
        { Qt::Key_0, Key_0 },
        { Qt::Key_1, Key_1 },
        { Qt::Key_2, Key_2 },
        { Qt::Key_3, Key_3 },
        { Qt::Key_4, Key_4 },
        { Qt::Key_5, Key_5 },
        { Qt::Key_6, Key_6 },
        { Qt::Key_7, Key_7 },
        { Qt::Key_8, Key_8 },
        { Qt::Key_9, Key_9 },
        { Qt::Key_A, Key_A },
        { Qt::Key_Alt, Key_Alt },
        { Qt::Key_Ampersand, Key_Ampersand },
        { Qt::Key_Apostrophe, Key_Apostrophe },
        { Qt::Key_AsciiCircum, Key_Circumflex },
        { Qt::Key_AsciiTilde, Key_Tilde },
        { Qt::Key_Asterisk, Key_Asterisk },
        { Qt::Key_At, Key_AtSign },
        { Qt::Key_B, Key_B },
        { Qt::Key_Backslash, Key_Backslash },
        { Qt::Key_Backspace, Key_Backspace },
        { Qt::Key_Bar, Key_Pipe },
        { Qt::Key_BraceLeft, Key_LeftBrace },
        { Qt::Key_BraceRight, Key_RightBrace },
        { Qt::Key_BracketLeft, Key_LeftBracket },
        { Qt::Key_BracketRight, Key_RightBracket },
        { Qt::Key_C, Key_C },
        { Qt::Key_CapsLock, Key_CapsLock },
        { Qt::Key_Colon, Key_Colon },
        { Qt::Key_Comma, Key_Comma },
        { Qt::Key_Control, Key_Control },
        { Qt::Key_D, Key_D },
        { Qt::Key_Delete, Key_Delete },
        { Qt::Key_Dollar, Key_Dollar },
        { Qt::Key_Down, Key_Down },
        { Qt::Key_E, Key_E },
        { Qt::Key_End, Key_End },
        { Qt::Key_Equal, Key_Equal },
        { Qt::Key_Enter, Key_Return },
        { Qt::Key_Escape, Key_Escape },
        { Qt::Key_Exclam, Key_ExclamationPoint },
        { Qt::Key_exclamdown, Key_ExclamationPoint },
        { Qt::Key_F, Key_F },
        { Qt::Key_F1, Key_F1 },
        { Qt::Key_F10, Key_F10 },
        { Qt::Key_F11, Key_F11 },
        { Qt::Key_F12, Key_F12 },
        { Qt::Key_F2, Key_F2 },
        { Qt::Key_F3, Key_F3 },
        { Qt::Key_F4, Key_F4 },
        { Qt::Key_F5, Key_F5 },
        { Qt::Key_F6, Key_F6 },
        { Qt::Key_F7, Key_F7 },
        { Qt::Key_F8, Key_F8 },
        { Qt::Key_F9, Key_F9 },
        { Qt::Key_G, Key_G },
        { Qt::Key_Greater, Key_GreaterThan },
        { Qt::Key_H, Key_H },
        { Qt::Key_Home, Key_Home },
        { Qt::Key_I, Key_I },
        { Qt::Key_Insert, Key_Insert },
        { Qt::Key_J, Key_J },
        { Qt::Key_K, Key_K },
        { Qt::Key_L, Key_L },
        { Qt::Key_Left, Key_Left },
        { Qt::Key_Less, Key_LessThan },
        { Qt::Key_M, Key_M },
        { Qt::Key_Menu, Key_Menu },
        { Qt::Key_Meta, Key_Super },
        { Qt::Key_Minus, Key_Minus },
        { Qt::Key_N, Key_N },
        { Qt::Key_NumberSign, Key_Hashtag },
        { Qt::Key_NumLock, Key_NumLock },
        { Qt::Key_O, Key_O },
        { Qt::Key_P, Key_P },
        { Qt::Key_PageDown, Key_PageDown },
        { Qt::Key_PageUp, Key_PageUp },
        { Qt::Key_ParenLeft, Key_LeftParen },
        { Qt::Key_ParenRight, Key_RightParen },
        { Qt::Key_Percent, Key_Percent },
        { Qt::Key_Period, Key_Period },
        { Qt::Key_Plus, Key_Plus },
        { Qt::Key_Print, Key_PrintScreen },
        { Qt::Key_Q, Key_Q },
        { Qt::Key_Question, Key_QuestionMark },
        { Qt::Key_QuoteDbl, Key_DoubleQuote },
        { Qt::Key_QuoteLeft, Key_Backtick },
        { Qt::Key_R, Key_R },
        { Qt::Key_Return, Key_Return },
        { Qt::Key_Right, Key_Right },
        { Qt::Key_S, Key_S },
        { Qt::Key_ScrollLock, Key_ScrollLock },
        { Qt::Key_Semicolon, Key_Semicolon },
        { Qt::Key_Shift, Key_LeftShift },
        { Qt::Key_Slash, Key_Slash },
        { Qt::Key_Space, Key_Space },
        { Qt::Key_Super_L, Key_Super },
        { Qt::Key_Super_R, Key_Super },
        { Qt::Key_SysReq, Key_SysRq },
        { Qt::Key_T, Key_T },
        { Qt::Key_Tab, Key_Tab },
        { Qt::Key_U, Key_U },
        { Qt::Key_Underscore, Key_Underscore },
        { Qt::Key_Up, Key_Up },
        { Qt::Key_V, Key_V },
        { Qt::Key_W, Key_W },
        { Qt::Key_X, Key_X },
        { Qt::Key_Y, Key_Y },
        { Qt::Key_Z, Key_Z },
    };

    for (auto const& mapping : mappings) {
        if (event.key() == mapping.qt_key)
            return mapping.serenity_key;
    }
    return Key_Invalid;
}

void WebContentView::wheelEvent(QWheelEvent* event)
{
    if (!event->modifiers().testFlag(Qt::ControlModifier)) {
        Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
        auto button = get_button_from_qt_event(*event);
        auto buttons = get_buttons_from_qt_event(*event);
        auto modifiers = get_modifiers_from_qt_mouse_event(*event);

        auto num_pixels = -event->pixelDelta();
        if (!num_pixels.isNull()) {
            client().async_mouse_wheel(to_content_position(position), button, buttons, modifiers, num_pixels.x(), num_pixels.y());
        } else {
            auto num_degrees = -event->angleDelta();
            float delta_x = -num_degrees.x() / 120;
            float delta_y = num_degrees.y() / 120;
            auto step_x = delta_x * QApplication::wheelScrollLines() * devicePixelRatio();
            auto step_y = delta_y * QApplication::wheelScrollLines() * devicePixelRatio();
            int scroll_step_size = verticalScrollBar()->singleStep();
            client().async_mouse_wheel(to_content_position(position), button, buttons, modifiers, step_x * scroll_step_size, step_y * scroll_step_size);
        }
        event->accept();
        return;
    }
    event->ignore();
}

void WebContentView::mouseMoveEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto buttons = get_buttons_from_qt_event(*event);
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    client().async_mouse_move(to_content_position(position), 0, buttons, modifiers);
}

void WebContentView::mousePressEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    if (button == 0) {
        // We could not convert Qt buttons to something that Lagom can
        // recognize - don't even bother propagating this to the web engine
        // as it will not handle it anyway, and it will (currently) assert
        return;
    }
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    auto buttons = get_buttons_from_qt_event(*event);
    client().async_mouse_down(to_content_position(position), button, buttons, modifiers);
}

void WebContentView::mouseReleaseEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);

    if (event->button() & Qt::MouseButton::BackButton) {
        if (on_navigate_back)
            on_navigate_back();
    } else if (event->button() & Qt::MouseButton::ForwardButton) {
        if (on_navigate_forward)
            on_navigate_forward();
    }

    if (button == 0) {
        // We could not convert Qt buttons to something that Lagom can
        // recognize - don't even bother propagating this to the web engine
        // as it will not handle it anyway, and it will (currently) assert
        return;
    }
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    auto buttons = get_buttons_from_qt_event(*event);
    client().async_mouse_up(to_content_position(position), button, buttons, modifiers);
}

void WebContentView::mouseDoubleClickEvent(QMouseEvent* event)
{
    Gfx::IntPoint position(event->position().x() / m_inverse_pixel_scaling_ratio, event->position().y() / m_inverse_pixel_scaling_ratio);
    auto button = get_button_from_qt_event(*event);
    if (button == 0) {
        // We could not convert Qt buttons to something that Lagom can
        // recognize - don't even bother propagating this to the web engine
        // as it will not handle it anyway, and it will (currently) assert
        return;
    }
    auto modifiers = get_modifiers_from_qt_mouse_event(*event);
    auto buttons = get_buttons_from_qt_event(*event);
    client().async_doubleclick(to_content_position(position), button, buttons, modifiers);
}

void WebContentView::dragEnterEvent(QDragEnterEvent* event)
{
    if (event->mimeData()->hasUrls())
        event->acceptProposedAction();
}

void WebContentView::dropEvent(QDropEvent* event)
{
    VERIFY(event->mimeData()->hasUrls());
    emit urls_dropped(event->mimeData()->urls());
    event->acceptProposedAction();
}

void WebContentView::keyPressEvent(QKeyEvent* event)
{
    switch (event->key()) {
    case Qt::Key_Left:
    case Qt::Key_Right:
    case Qt::Key_Up:
    case Qt::Key_Down:
    case Qt::Key_PageUp:
    case Qt::Key_PageDown:
        QAbstractScrollArea::keyPressEvent(event);
        break;
    default:
        break;
    }

    if (event->key() == Qt::Key_Backtab) {
        // NOTE: Qt transforms Shift+Tab into a "Backtab", so we undo that transformation here.
        client().async_key_down(KeyCode::Key_Tab, Mod_Shift, '\t');
        return;
    }

    auto text = event->text();
    auto point = text.isEmpty() ? 0u : event->text()[0].unicode();
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    client().async_key_down(keycode, modifiers, point);
}

void WebContentView::keyReleaseEvent(QKeyEvent* event)
{
    auto text = event->text();
    auto point = text.isEmpty() ? 0u : event->text()[0].unicode();
    auto keycode = get_keycode_from_qt_keyboard_event(*event);
    auto modifiers = get_modifiers_from_qt_keyboard_event(*event);
    client().async_key_up(keycode, modifiers, point);
}

void WebContentView::focusInEvent(QFocusEvent*)
{
    client().async_set_has_focus(true);
}

void WebContentView::focusOutEvent(QFocusEvent*)
{
    client().async_set_has_focus(false);
}

void WebContentView::paintEvent(QPaintEvent*)
{
    QPainter painter(viewport());
    painter.scale(m_inverse_pixel_scaling_ratio, m_inverse_pixel_scaling_ratio);

    Gfx::Bitmap const* bitmap = nullptr;
    Gfx::IntSize bitmap_size;

    if (m_client_state.has_usable_bitmap) {
        bitmap = m_client_state.front_bitmap.bitmap.ptr();
        bitmap_size = m_client_state.front_bitmap.last_painted_size;

    } else {
        bitmap = m_backup_bitmap.ptr();
        bitmap_size = m_backup_bitmap_size;
    }

    if (bitmap) {
        QImage q_image(bitmap->scanline_u8(0), bitmap->width(), bitmap->height(), QImage::Format_RGB32);
        painter.drawImage(QPoint(0, 0), q_image, QRect(0, 0, bitmap_size.width(), bitmap_size.height()));

        if (bitmap_size.width() < width()) {
            painter.fillRect(bitmap_size.width(), 0, width() - bitmap_size.width(), bitmap->height(), palette().base());
        }
        if (bitmap_size.height() < height()) {
            painter.fillRect(0, bitmap_size.height(), width(), height() - bitmap_size.height(), palette().base());
        }

        return;
    }

    painter.fillRect(rect(), palette().base());
}

void WebContentView::resizeEvent(QResizeEvent* event)
{
    QAbstractScrollArea::resizeEvent(event);
    update_viewport_rect();
    handle_resize();
}

void WebContentView::set_viewport_rect(Gfx::IntRect rect)
{
    m_viewport_rect = rect;
    client().async_set_viewport_rect(rect);
}

void WebContentView::set_window_size(Gfx::IntSize size)
{
    client().async_set_window_size(size);
}

void WebContentView::set_window_position(Gfx::IntPoint position)
{
    client().async_set_window_position(position);
}

void WebContentView::update_viewport_rect()
{
    auto scaled_width = int(viewport()->width() / m_inverse_pixel_scaling_ratio);
    auto scaled_height = int(viewport()->height() / m_inverse_pixel_scaling_ratio);
    Gfx::IntRect rect(max(0, horizontalScrollBar()->value()), max(0, verticalScrollBar()->value()), scaled_width, scaled_height);

    set_viewport_rect(rect);

    request_repaint();
}

void WebContentView::update_zoom()
{
    client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio * m_zoom_level);
    update_viewport_rect();
    request_repaint();
}

void WebContentView::showEvent(QShowEvent* event)
{
    QAbstractScrollArea::showEvent(event);
    client().async_set_system_visibility_state(true);
}

void WebContentView::hideEvent(QHideEvent* event)
{
    QAbstractScrollArea::hideEvent(event);
    client().async_set_system_visibility_state(false);
}

static Core::AnonymousBuffer make_system_theme_from_qt_palette(QWidget& widget, WebContentView::PaletteMode mode)
{
    auto qt_palette = widget.palette();

    auto theme_file = mode == WebContentView::PaletteMode::Default ? "Default"sv : "Dark"sv;
    auto theme = Gfx::load_system_theme(DeprecatedString::formatted("{}/res/themes/{}.ini", s_serenity_resource_root, theme_file)).release_value_but_fixme_should_propagate_errors();
    auto palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme);
    auto palette = Gfx::Palette(move(palette_impl));

    auto translate = [&](Gfx::ColorRole gfx_color_role, QPalette::ColorRole qt_color_role) {
        auto new_color = Gfx::Color::from_argb(qt_palette.color(qt_color_role).rgba());
        palette.set_color(gfx_color_role, new_color);
    };

    translate(Gfx::ColorRole::ThreedHighlight, QPalette::ColorRole::Light);
    translate(Gfx::ColorRole::ThreedShadow1, QPalette::ColorRole::Mid);
    translate(Gfx::ColorRole::ThreedShadow2, QPalette::ColorRole::Dark);
    translate(Gfx::ColorRole::HoverHighlight, QPalette::ColorRole::Light);
    translate(Gfx::ColorRole::Link, QPalette::ColorRole::Link);
    translate(Gfx::ColorRole::VisitedLink, QPalette::ColorRole::LinkVisited);
    translate(Gfx::ColorRole::Button, QPalette::ColorRole::Button);
    translate(Gfx::ColorRole::ButtonText, QPalette::ColorRole::ButtonText);
    translate(Gfx::ColorRole::Selection, QPalette::ColorRole::Highlight);
    translate(Gfx::ColorRole::SelectionText, QPalette::ColorRole::HighlightedText);

    palette.set_flag(Gfx::FlagRole::IsDark, is_using_dark_system_theme(widget));

    return theme;
}

void WebContentView::update_palette(PaletteMode mode)
{
    client().async_update_system_theme(make_system_theme_from_qt_palette(*this, mode));
}

void WebContentView::create_client(WebView::EnableCallgrindProfiling enable_callgrind_profiling)
{
    m_client_state = {};

    auto candidate_web_content_paths = get_paths_for_helper_process("WebContent"sv).release_value_but_fixme_should_propagate_errors();
    auto new_client = launch_web_content_process(*this, candidate_web_content_paths, enable_callgrind_profiling, WebView::IsLayoutTestMode::No, m_use_lagom_networking).release_value_but_fixme_should_propagate_errors();

    m_client_state.client = new_client;
    m_client_state.client->on_web_content_process_crash = [this] {
        Core::deferred_invoke([this] {
            handle_web_content_process_crash();
        });
    };

    m_client_state.client_handle = Web::Crypto::generate_random_uuid().release_value_but_fixme_should_propagate_errors();
    client().async_set_window_handle(m_client_state.client_handle);

    client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio);
    update_palette();
    client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());

    auto screens = QGuiApplication::screens();

    if (!screens.empty()) {
        Vector<Gfx::IntRect> screen_rects;

        for (auto const& screen : screens) {
            auto geometry = screen->geometry();

            screen_rects.append(Gfx::IntRect(geometry.x(), geometry.y(), geometry.width(), geometry.height()));
        }

        // FIXME: Update the screens again when QGuiApplication::screenAdded/Removed signals are emitted

        // NOTE: The first item in QGuiApplication::screens is always the primary screen.
        //       This is not specified in the documentation but QGuiApplication::primaryScreen
        //       always returns the first item in the list if it isn't empty.
        client().async_update_screen_rects(screen_rects, 0);
    }

    if (!m_webdriver_content_ipc_path.is_empty())
        client().async_connect_to_webdriver(m_webdriver_content_ipc_path);
}

void WebContentView::notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id, Gfx::IntSize size)
{
    if (m_client_state.back_bitmap.id == bitmap_id) {
        m_client_state.has_usable_bitmap = true;
        m_client_state.back_bitmap.pending_paints--;
        m_client_state.back_bitmap.last_painted_size = size;
        swap(m_client_state.back_bitmap, m_client_state.front_bitmap);
        // We don't need the backup bitmap anymore, so drop it.
        m_backup_bitmap = nullptr;
        viewport()->update();

        if (m_client_state.got_repaint_requests_while_painting) {
            m_client_state.got_repaint_requests_while_painting = false;
            request_repaint();
        }
    }
}

void WebContentView::notify_server_did_invalidate_content_rect(Badge<WebContentClient>, [[maybe_unused]] Gfx::IntRect const& content_rect)
{
    request_repaint();
}

void WebContentView::notify_server_did_change_selection(Badge<WebContentClient>)
{
    request_repaint();
}

void WebContentView::notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor)
{
    switch (cursor) {
    case Gfx::StandardCursor::Hidden:
        setCursor(Qt::BlankCursor);
        break;
    case Gfx::StandardCursor::Arrow:
        setCursor(Qt::ArrowCursor);
        break;
    case Gfx::StandardCursor::Crosshair:
        setCursor(Qt::CrossCursor);
        break;
    case Gfx::StandardCursor::IBeam:
        setCursor(Qt::IBeamCursor);
        break;
    case Gfx::StandardCursor::ResizeHorizontal:
        setCursor(Qt::SizeHorCursor);
        break;
    case Gfx::StandardCursor::ResizeVertical:
        setCursor(Qt::SizeVerCursor);
        break;
    case Gfx::StandardCursor::ResizeDiagonalTLBR:
        setCursor(Qt::SizeFDiagCursor);
        break;
    case Gfx::StandardCursor::ResizeDiagonalBLTR:
        setCursor(Qt::SizeBDiagCursor);
        break;
    case Gfx::StandardCursor::ResizeColumn:
        setCursor(Qt::SplitHCursor);
        break;
    case Gfx::StandardCursor::ResizeRow:
        setCursor(Qt::SplitVCursor);
        break;
    case Gfx::StandardCursor::Hand:
        setCursor(Qt::PointingHandCursor);
        break;
    case Gfx::StandardCursor::Help:
        setCursor(Qt::WhatsThisCursor);
        break;
    case Gfx::StandardCursor::Drag:
        setCursor(Qt::ClosedHandCursor);
        break;
    case Gfx::StandardCursor::DragCopy:
        setCursor(Qt::DragCopyCursor);
        break;
    case Gfx::StandardCursor::Move:
        setCursor(Qt::DragMoveCursor);
        break;
    case Gfx::StandardCursor::Wait:
        setCursor(Qt::BusyCursor);
        break;
    case Gfx::StandardCursor::Disallowed:
        setCursor(Qt::ForbiddenCursor);
        break;
    case Gfx::StandardCursor::Eyedropper:
    case Gfx::StandardCursor::Zoom:
        // FIXME: No corresponding Qt cursors, default to Arrow
    default:
        setCursor(Qt::ArrowCursor);
        break;
    }
}

void WebContentView::notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize content_size)
{
    verticalScrollBar()->setMinimum(0);
    verticalScrollBar()->setMaximum(content_size.height() - m_viewport_rect.height());
    verticalScrollBar()->setPageStep(m_viewport_rect.height());
    horizontalScrollBar()->setMinimum(0);
    horizontalScrollBar()->setMaximum(content_size.width() - m_viewport_rect.width());
    horizontalScrollBar()->setPageStep(m_viewport_rect.width());
}

void WebContentView::notify_server_did_request_scroll(Badge<WebContentClient>, i32 x_delta, i32 y_delta)
{
    horizontalScrollBar()->setValue(max(0, horizontalScrollBar()->value() + x_delta));
    verticalScrollBar()->setValue(max(0, verticalScrollBar()->value() + y_delta));
}

void WebContentView::notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint scroll_position)
{
    horizontalScrollBar()->setValue(scroll_position.x());
    verticalScrollBar()->setValue(scroll_position.y());
}

void WebContentView::notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const& rect)
{
    if (m_viewport_rect.contains(rect))
        return;

    if (rect.top() < m_viewport_rect.top())
        verticalScrollBar()->setValue(rect.top());
    else if (rect.top() > m_viewport_rect.top() && rect.bottom() > m_viewport_rect.bottom())
        verticalScrollBar()->setValue(rect.bottom() - m_viewport_rect.height());
}

void WebContentView::notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint content_position, DeprecatedString const& tooltip)
{
    auto widget_position = to_widget_position(content_position);
    QToolTip::showText(
        mapToGlobal(QPoint(widget_position.x(), widget_position.y())),
        qstring_from_ak_deprecated_string(tooltip),
        this);
}

void WebContentView::notify_server_did_leave_tooltip_area(Badge<WebContentClient>)
{
    QToolTip::hideText();
}

void WebContentView::notify_server_did_request_file(Badge<WebContentClient>, DeprecatedString const& path, i32 request_id)
{
    auto file = Core::File::open(path, Core::File::OpenMode::Read);
    if (file.is_error())
        client().async_handle_file_return(file.error().code(), {}, request_id);
    else
        client().async_handle_file_return(0, IPC::File(*file.value()), request_id);
}

Gfx::IntRect WebContentView::viewport_rect() const
{
    return m_viewport_rect;
}

Gfx::IntPoint WebContentView::to_content_position(Gfx::IntPoint widget_position) const
{
    return widget_position.translated(max(0, horizontalScrollBar()->value()), max(0, verticalScrollBar()->value()));
}

Gfx::IntPoint WebContentView::to_widget_position(Gfx::IntPoint content_position) const
{
    return content_position.translated(-(max(0, horizontalScrollBar()->value())), -(max(0, verticalScrollBar()->value())));
}

bool WebContentView::event(QEvent* event)
{
    // NOTE: We have to implement event() manually as Qt's focus navigation mechanism
    //       eats all the Tab key presses by default.

    if (event->type() == QEvent::KeyPress) {
        keyPressEvent(static_cast<QKeyEvent*>(event));
        return true;
    }
    if (event->type() == QEvent::KeyRelease) {
        keyReleaseEvent(static_cast<QKeyEvent*>(event));
        return true;
    }

    if (event->type() == QEvent::PaletteChange) {
        update_palette();
        request_repaint();
        return QAbstractScrollArea::event(event);
    }

    return QAbstractScrollArea::event(event);
}

void WebContentView::notify_server_did_finish_handling_input_event(bool event_was_accepted)
{
    // FIXME: Currently Ladybird handles the keyboard shortcuts before passing the event to web content, so
    //        we don't need to do anything here. But we'll need to once we start asking web content first.
    (void)event_was_accepted;
}

ErrorOr<String> WebContentView::dump_layout_tree()
{
    return String::from_deprecated_string(client().dump_layout_tree());
}

}
