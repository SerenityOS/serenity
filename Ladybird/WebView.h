/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <QAbstractScrollArea>
#include <QPointer>

class QTextEdit;
class QLineEdit;

class HeadlessBrowserPageClient;

class WebView final : public QAbstractScrollArea {
    Q_OBJECT
public:
    WebView();
    virtual ~WebView() override;

    void load(String const& url);
    void reload();

    virtual void paintEvent(QPaintEvent*) override;
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void mouseMoveEvent(QMouseEvent*) override;
    virtual void mousePressEvent(QMouseEvent*) override;
    virtual void mouseReleaseEvent(QMouseEvent*) override;
    virtual void keyPressEvent(QKeyEvent* event) override;
    virtual void keyReleaseEvent(QKeyEvent* event) override;
    void debug_request(String const& request, String const& argument);

    String source() const;

    void run_javascript(String const& js_source) const;

    void did_output_js_console_message(i32 message_index);
    void did_get_js_console_messages(i32 start_index, Vector<String> message_types, Vector<String> messages);

    void show_js_console();

signals:
    void linkHovered(QString, int timeout = 0);
    void linkUnhovered();
    void loadStarted(const URL&);
    void title_changed(QString);
    void favicon_changed(QIcon);

private:
    Gfx::IntPoint to_content(Gfx::IntPoint) const;

    OwnPtr<HeadlessBrowserPageClient> m_page_client;

    qreal m_inverse_pixel_scaling_ratio { 1.0 };
    bool m_should_show_line_box_borders { false };

    QPointer<QWidget> m_js_console_widget;
    QTextEdit* m_js_console_output_edit { nullptr };
    QLineEdit* m_js_console_input_edit { nullptr };
};
