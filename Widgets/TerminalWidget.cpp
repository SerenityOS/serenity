#include "TerminalWidget.h"
#include "Font.h"
#include "Painter.h"
#include <unistd.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <fcntl.h>

extern int g_fd;
TerminalWidget* g_tw;

TerminalWidget::TerminalWidget(Widget* parent)
    : Widget(parent)
{
    g_tw = this;

    auto& font = Font::defaultFont();
    
    setRect({ 100, 300, (columns() * font.glyphWidth()) + 4, (rows() * font.glyphHeight()) + 4 });

    printf("rekt: %d x %d\n", width(), height());
    m_screen = new CharacterWithAttributes[rows() * columns()]; 
    for (unsigned row = 0; row < m_rows; ++row) {
        for (unsigned column = 0; column < m_columns; ++column) {
            at(row, column).character = ' ';
            at(row, column).attribute = 0x07;
        }
    }
    g_fd = getpt();
    grantpt(g_fd);
    unlockpt(g_fd);
    char buf[1024];
    ptsname_r(g_fd, buf, sizeof(buf));

    if (fork() == 0) {
        close(g_fd);
        setsid();
        int fd = open(buf, O_RDWR);
        dup2(fd, 0);
        dup2(fd, 1);
        dup2(fd, 2);
        signal(SIGWINCH, SIG_IGN);
        ioctl(fd, TIOCSCTTY);
        execl("/bin/bash", "bash", nullptr);
        ASSERT_NOT_REACHED();
    }

    signal(SIGCHLD, SIG_IGN);
}

TerminalWidget::~TerminalWidget()
{
}

CharacterWithAttributes& TerminalWidget::at(unsigned row, unsigned column)
{
    ASSERT(m_screen);
    ASSERT(row < m_rows);
    ASSERT(column < m_columns);
    return m_screen[row * columns() + column];
}

void TerminalWidget::onPaint(PaintEvent&)
{
    Painter painter(*this);
    painter.fillRect({ 0, 0, width(), height() }, Color(0, 0, 0));

    auto& font = Font::defaultFont();

    char buf[2] = { 0, 0 };
    for (unsigned row = 0; row < m_rows; ++row) {
        int y = row * font.glyphHeight();
        for (unsigned column = 0; column < m_columns; ++column) {
            int x = column * font.glyphWidth();
            buf[0] = at(row, column).character;
            painter.drawText({ x + 2, y + 2, width(), font.glyphHeight() }, buf, Painter::TextAlignment::TopLeft, Color(0xa0, 0xa0, 0xa0));
        }
    }
}

void TerminalWidget::onReceive(const ByteBuffer& buffer)
{
    for (unsigned i = 0; i < buffer.size(); ++i) {
        onReceive(buffer[i]);
    }
}

void TerminalWidget::onReceive(byte ch)
{
    //printf("receive %02x\n", ch);
    auto scrollScreen = [&] () {
        memmove(m_screen, m_screen + columns(), (m_rows - 1) * columns() * sizeof(CharacterWithAttributes));
        memset(m_screen + (m_rows - 1) * columns(), ' ', columns() * sizeof(CharacterWithAttributes) * 2);
    };

    auto addChar = [&] (byte ch) {
        at(m_cursorRow, m_cursorColumn).character = ch;
        if (++m_cursorColumn >= m_columns) {
            m_cursorColumn = 0;
            if (m_cursorRow < (m_rows - 1)) {
                ++m_cursorRow;
            } else {
                scrollScreen();
            }
        }
    };

    if (ch == '\n') {
        if (m_cursorRow < (m_rows - 1)) {
            ++m_cursorRow;
        } else {
            scrollScreen();
        }
    } else if (ch == '\r') {
        m_cursorColumn = 0;
    } else if (ch == '\t') {
        while ((m_cursorColumn % 8) != 0 && m_cursorColumn < m_columns) {
            addChar(' ');
        }
    } else {
        addChar(ch);
    }
    update();
}

void TerminalWidget::onKeyDown(KeyEvent& event)
{
    char buf[] = { 0, 0 };
    buf[0] = event.key();
    write(g_fd, buf, 2);
    return Widget::onKeyDown(event);
}

void TerminalWidget::onKeyUp(KeyEvent& event)
{
    return Widget::onKeyUp(event);
}

