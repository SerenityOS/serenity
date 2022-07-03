/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <QAbstractScrollArea>

class HeadlessBrowserPageClient;

class WebView final : public QAbstractScrollArea {
    Q_OBJECT
public:
    WebView();
    virtual ~WebView() override;

    void load(String const& url);

    virtual void paintEvent(QPaintEvent*) override;
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void mouseMoveEvent(QMouseEvent*) override;
    virtual void mousePressEvent(QMouseEvent*) override;
    virtual void mouseReleaseEvent(QMouseEvent*) override;

signals:
    void linkHovered(QString, int timeout = 0);
    void linkUnhovered();

private:
    OwnPtr<HeadlessBrowserPageClient> m_page_client;
};
