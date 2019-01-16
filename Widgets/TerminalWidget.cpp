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

    setWindowRelativeRect({ 0, 0, int(columns() * font().glyph_width()) + 4, int(rows() * font().glyph_height()) + 4 });

    printf("rekt: %d x %d\n", width(), height());
    m_screen = new CharacterWithAttributes[rows() * columns()]; 
    for (unsigned row = 0; row < m_rows; ++row) {
        for (unsigned column = 0; column < m_columns; ++column) {
            at(row, column).character = ' ';
            at(row, column).attribute = 0x07;
        }
    }

#if __APPLE__
    g_fd = posix_openpt(O_RDWR);
#else
    g_fd = getpt();
#endif

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

void TerminalWidget::paintEvent(PaintEvent&)
{
    Painter painter(*this);
    painter.fill_rect(rect(), Color::Black);

    char buf[2] = { 0, 0 };
    for (unsigned row = 0; row < m_rows; ++row) {
        int y = row * font().glyph_height();
        for (unsigned column = 0; column < m_columns; ++column) {
            int x = column * font().glyph_width();
            buf[0] = at(row, column).character;
            painter.draw_text({ x + 2, y + 2, width(), font().glyph_height() }, buf, Painter::TextAlignment::TopLeft, Color(0xa0, 0xa0, 0xa0));
        }
    }

    if (m_belling)
        painter.draw_rect(rect(), Color::Red);
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
        memset(m_screen + (m_rows - 1) * columns(), ' ', columns() * sizeof(CharacterWithAttributes));
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

    switch (ch) {
        case '\n':
            if (m_cursorRow < (m_rows - 1)) {
                ++m_cursorRow;
            } else {
                scrollScreen();
            }
            break;
        case '\r':
            m_cursorColumn = 0;
            break;
        case '\t':
            // FIXME: Respect terminal tab stops.
            while ((m_cursorColumn % 8) != 0 && m_cursorColumn < m_columns) {
                addChar(' ');
            break;
        case '\a':
            bell();
            break;
        case 8:
            if (m_cursorColumn > 0) {
                --m_cursorColumn;
                at(m_cursorRow, m_cursorColumn).character = ' ';
            }
            break;
        case 27:
            printf("TerminalWidget: got escape!\n");
            break;
        default:
            addChar(ch);
            break;
        }
    }
    update();
}

void TerminalWidget::keyDownEvent(KeyEvent& event)
{
    if (event.text().is_empty())
        return;
    write(g_fd, event.text().characters(), event.text().length());
}

void TerminalWidget::keyUpEvent(KeyEvent& event)
{
    return Widget::keyUpEvent(event);
}

void TerminalWidget::bell()
{
    if (m_belling)
        stopTimer();
    startTimer(250);
    m_belling = true;
    update();
}

void TerminalWidget::timerEvent(TimerEvent&)
{
    m_belling = false;
    stopTimer();
    update();
}
