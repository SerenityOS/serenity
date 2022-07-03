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

private:
    OwnPtr<HeadlessBrowserPageClient> m_page_client;
};
