#include <LibCore/Forward.h>
#include <QIcon>
#include <QLineEdit>
#include <QMainWindow>
#include <QToolBar>

#pragma once

class WebView;

class BrowserWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit BrowserWindow(Core::EventLoop&);

    WebView& view() { return *m_view; }

    virtual void closeEvent(QCloseEvent*) override;

public slots:
    void location_edit_return_pressed();
    void page_title_changed(QString);
    void page_favicon_changed(QIcon);

private:
    QToolBar* m_toolbar { nullptr };
    QLineEdit* m_location_edit { nullptr };
    WebView* m_view { nullptr };
    Core::EventLoop& m_event_loop;
};
