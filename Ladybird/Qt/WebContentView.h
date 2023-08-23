/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/URL.h>
#include <Ladybird/Types.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWebView/ViewImplementation.h>
#include <QAbstractScrollArea>
#include <QUrl>

class QTextEdit;
class QLineEdit;

namespace WebView {
class WebContentClient;
}

using WebView::WebContentClient;

namespace Ladybird {

class Tab;

class WebContentView final
    : public QAbstractScrollArea
    , public WebView::ViewImplementation {
    Q_OBJECT
public:
    explicit WebContentView(StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling, UseLagomNetworking);
    virtual ~WebContentView() override;

    Function<String(const AK::URL&, Web::HTML::ActivateTab)> on_tab_open_request;

    virtual void paintEvent(QPaintEvent*) override;
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void wheelEvent(QWheelEvent*) override;
    virtual void mouseMoveEvent(QMouseEvent*) override;
    virtual void mousePressEvent(QMouseEvent*) override;
    virtual void mouseReleaseEvent(QMouseEvent*) override;
    virtual void mouseDoubleClickEvent(QMouseEvent*) override;
    virtual void dragEnterEvent(QDragEnterEvent*) override;
    virtual void dropEvent(QDropEvent*) override;
    virtual void keyPressEvent(QKeyEvent* event) override;
    virtual void keyReleaseEvent(QKeyEvent* event) override;
    virtual void showEvent(QShowEvent*) override;
    virtual void hideEvent(QHideEvent*) override;
    virtual void focusInEvent(QFocusEvent*) override;
    virtual void focusOutEvent(QFocusEvent*) override;
    virtual bool event(QEvent*) override;

    ErrorOr<String> dump_layout_tree();

    void set_viewport_rect(Gfx::IntRect);
    void set_window_size(Gfx::IntSize);
    void set_window_position(Gfx::IntPoint);

    enum class PaletteMode {
        Default,
        Dark,
    };
    void update_palette(PaletteMode = PaletteMode::Default);

    virtual void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint, DeprecatedString const&) override;
    virtual void notify_server_did_leave_tooltip_area(Badge<WebContentClient>) override;
    virtual void notify_server_did_finish_handling_input_event(bool event_was_accepted) override;

signals:
    void urls_dropped(QList<QUrl> const&);

private:
    // ^WebView::ViewImplementation
    virtual void create_client(WebView::EnableCallgrindProfiling = WebView::EnableCallgrindProfiling::No) override;
    virtual void update_zoom() override;
    virtual Gfx::IntRect viewport_rect() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    void update_viewport_rect();
    void update_cursor(Gfx::StandardCursor cursor);

    qreal m_inverse_pixel_scaling_ratio { 1.0 };
    bool m_should_show_line_box_borders { false };
    UseLagomNetworking m_use_lagom_networking {};

    Gfx::IntRect m_viewport_rect;

    StringView m_webdriver_content_ipc_path;
};

}
