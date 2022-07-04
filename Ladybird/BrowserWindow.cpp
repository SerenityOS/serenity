#include "BrowserWindow.h"
#include "WebView.h"
#include <QStatusBar>

BrowserWindow::BrowserWindow()
{
    m_toolbar = new QToolBar;
    m_toolbar->setFixedHeight(28);
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
