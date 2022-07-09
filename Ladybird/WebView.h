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

    void debug_request(String const& request, String const& argument);

    String source() const;

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
};
