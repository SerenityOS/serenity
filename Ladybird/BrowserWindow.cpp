#include "BrowserWindow.h"
#include "WebView.h"
#include <LibCore/EventLoop.h>
#include <QAction>
#include <QStatusBar>

extern String s_serenity_resource_root;

BrowserWindow::BrowserWindow(Core::EventLoop& event_loop)
    : m_event_loop(event_loop)
{
    m_toolbar = new QToolBar;

    auto reload_icon_path = QString("%1/res/icons/16x16/reload.png").arg(s_serenity_resource_root.characters());
    auto* reload_action = new QAction(QIcon(reload_icon_path), "Reload");
    reload_action->setShortcut(QKeySequence("Ctrl+R"));
    m_toolbar->addAction(reload_action);

    m_location_edit = new QLineEdit;
    m_toolbar->addWidget(m_location_edit);

    addToolBar(m_toolbar);

    m_view = new WebView;
    setCentralWidget(m_view);

    QObject::connect(m_view, &WebView::linkHovered, statusBar(), &QStatusBar::showMessage);
    QObject::connect(m_view, &WebView::linkUnhovered, statusBar(), &QStatusBar::clearMessage);

    QObject::connect(m_view, &WebView::loadStarted, m_location_edit, &QLineEdit::setText);
    QObject::connect(m_location_edit, &QLineEdit::returnPressed, this, &BrowserWindow::location_edit_return_pressed);
    QObject::connect(m_view, &WebView::title_changed, this, &BrowserWindow::page_title_changed);
    QObject::connect(m_view, &WebView::favicon_changed, this, &BrowserWindow::page_favicon_changed);

    QObject::connect(reload_action, &QAction::triggered, this, &BrowserWindow::reload);
}

void BrowserWindow::location_edit_return_pressed()
{
    view().load(m_location_edit->text().toUtf8().data());
}

void BrowserWindow::page_title_changed(QString title)
{
    if (title.isEmpty())
        setWindowTitle("Ladybird");
    else
        setWindowTitle(QString("%1 - Ladybird").arg(title));
}

void BrowserWindow::page_favicon_changed(QIcon icon)
{
    setWindowIcon(icon);
}

void BrowserWindow::closeEvent(QCloseEvent* event)
{
    QWidget::closeEvent(event);

    // FIXME: Ladybird only supports one window at the moment. When we support
    //        multiple windows, we'll only want to fire off the quit event when
    //        all of the browser windows have closed.
    m_event_loop.quit(0);
}

void BrowserWindow::reload()
{
    view().reload();
}
