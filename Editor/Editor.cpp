#include "Editor.h"
#include "Document.h"
#include "InsertOperation.h"

#define _XOPEN_SOURCE_EXTENDED
#include <ncurses.h>
#include <stdarg.h>

static int statusbar_attributes;
static int ruler_attributes;

Editor::Editor()
{
    initscr();
    start_color();
    use_default_colors();

    init_pair(1, COLOR_WHITE, COLOR_BLUE);
    init_pair(2, COLOR_BLUE, -1);

    statusbar_attributes = COLOR_PAIR(1);
    ruler_attributes = COLOR_PAIR(2);

    raw();
    keypad(stdscr, true);
    timeout(10);
    noecho();
    refresh();
}

Editor::~Editor()
{
    //move(2, 2);
    //printw("*** Press any key to exit! ***");
    //getch();
    endwin();
}

void Editor::set_document(OwnPtr<Document>&& document)
{
    m_document = std::move(document);
    m_cursor.move_to(0, 0);
    m_scroll_position.move_to(0, 0);
}

void Editor::redraw()
{
    clear();
    if (!m_document)
        return;

    size_t window_height = getmaxy(stdscr);
    size_t window_width = getmaxx(stdscr);

    for (size_t row = 0; row < window_height - 1; ++row) {
        size_t current_document_line = m_scroll_position.line() + row;
        size_t current_document_column = m_scroll_position.column();

        move(row, 0);

        if (current_document_line >= m_document->line_count()) {
            printw("~");
        } else {
            attron(ruler_attributes);
            printw("%3d ", current_document_line);
            attroff(ruler_attributes);
            m_ruler_width = 4;
            size_t line_length = m_document->line(current_document_line).data().size();
            const char* line_data = m_document->line(current_document_line).data().c_str();
            if (m_scroll_position.column() < line_length)
                addnstr(line_data + m_scroll_position.column(), window_width - m_ruler_width);
        }
    }

    draw_status_bar();

    draw_cursor();
    refresh();
}

void Editor::draw_cursor()
{
    ssize_t cursor_row_on_screen = m_cursor.line() - m_scroll_position.line();
    ssize_t cursor_column_on_screen = m_cursor.column() - m_scroll_position.column();

    move(cursor_row_on_screen, cursor_column_on_screen + m_ruler_width);

}

void Editor::draw_status_bar()
{
    int old_background = getbkgd(stdscr);
    bkgdset(' ' | statusbar_attributes);
    
    size_t window_height = getmaxy(stdscr);
    size_t window_width = getmaxx(stdscr);

    move(window_height - 1, 0);
    clrtoeol();

    if (is_editing_document()) {
        attron(A_STANDOUT);
        printw("* Editing *");
        attroff(A_STANDOUT);
    } else if (is_editing_command()) {
        printw("\\%s", m_command.c_str());
    } else {
        attron(A_BOLD);
        addstr("~(^_^)~ ");
        if (m_status_text.size() > 0) {
            addstr(m_status_text.c_str());
        }
        attroff(A_BOLD);
    }

    move(window_height - 1, window_width - 20);
    printw("%zu, %zu", m_scroll_position.line(), m_scroll_position.column());

    move(window_height - 1, window_width - 8);
    printw("%zu, %zu", m_cursor.line(), m_cursor.column());
    attroff(statusbar_attributes);

    bkgdset(old_background);
}

int Editor::exec()
{
    while (!m_should_quit) {
        redraw();
        int ch = getch();
        if (ch == ERR) {
            continue;
            fprintf(stderr, "getch() returned ERR\n");
            break;
        }

        if (is_editing_document() || is_editing_command()) {
            if (ch == 27)
                set_mode(Idle);
            else {
                if (is_editing_document())
                    handle_document_key_press(ch);
                else
                    handle_command_key_press(ch);
            }
        } else {
            switch (ch) {
            case 'h': move_left(); break;
            case 'j': move_down(); break;
            case 'k': move_up(); break;
            case 'l': move_right(); break;
            case 'i': set_mode(EditingDocument); break;
            case 'I': move_to_start_of_line(); set_mode(EditingDocument); break;
            case 'A': move_to_end_of_line(); set_mode(EditingDocument); break;
            case '0': move_to_start_of_line(); break;
            case '$': move_to_end_of_line(); break;
            case 'a': move_right(); set_mode(EditingDocument); break;
            case 'x': erase_right(); break;
            case 'X': erase_left(); break;
            case '\\': set_mode(EditingCommand); break;
            }
        }
    }
    return 0;
}

void Editor::write_to_file()
{
    FILE* fp = fopen(m_document->path().c_str(), "w");
    if (!fp) {
        set_status_text("Failed to open %s for writing", m_document->path().c_str());
        return;
    }

    size_t bytes = 0;
    for (size_t i = 0; i < m_document->line_count(); ++i) {
        fwrite(m_document->line(i).data().c_str(), sizeof(char), m_document->line(i).length(), fp);
        bytes += m_document->line(i).length();
        if (i != m_document->line_count() - 1) {
            fputc('\n', fp);
            ++bytes;
        }
    }

    fclose(fp);
    set_status_text("Wrote %zu bytes across %zu lines", bytes, m_document->line_count());
}

void Editor::move_left()
{
    if (m_cursor.column() == 0)
        return;
    m_cursor.move_by(0, -1);
    update_scroll_position_if_needed();
}

void Editor::move_down()
{
    if (m_cursor.line() >= max_line())
        return;
    coalesce_current_line();
    m_cursor.move_by(1, 0);
    if (m_cursor.column() > max_column())
        m_cursor.set_column(max_column());

    update_scroll_position_if_needed();
}

void Editor::coalesce_current_line()
{
    m_document->line(m_cursor.line()).coalesce();
}

void Editor::move_up()
{
    if (m_cursor.line() == 0)
        return;
    coalesce_current_line();
    m_cursor.move_by(-1, 0);
    if (m_cursor.column() > max_column())
        m_cursor.set_column(max_column());

    update_scroll_position_if_needed();
}

void Editor::move_right()
{
    if (m_cursor.column() >= max_column())
        return;
    m_cursor.move_by(0, 1);
    update_scroll_position_if_needed();
}

void Editor::move_to_end_of_line()
{
    m_cursor.move_to(m_cursor.line(), m_document->line(m_cursor.line()).length());
    update_scroll_position_if_needed();
}

void Editor::move_to_start_of_line()
{
    m_cursor.move_to(m_cursor.line(), 0);
    update_scroll_position_if_needed();
}

size_t Editor::max_line() const
{
    return m_document->line_count() - 1;
}

size_t Editor::max_column() const
{
    return m_document->line(m_cursor.line()).data().size();
}

void Editor::update_scroll_position_if_needed()
{
    ssize_t max_row = getmaxy(stdscr) - 2;
    ssize_t max_column = getmaxx(stdscr) - 1 - m_ruler_width;

    ssize_t cursor_row_on_screen = m_cursor.line() - m_scroll_position.line();
    ssize_t cursor_column_on_screen = m_cursor.column() - m_scroll_position.column();

    // FIXME: Need to move by more than just 1 step sometimes!

    if (cursor_row_on_screen < 0) {
        m_scroll_position.move_by(-1, 0);
    }
    
    if (cursor_row_on_screen > max_row) {
        m_scroll_position.move_by(1, 0);
    }

    if (cursor_column_on_screen < 0) {
        m_scroll_position.move_by(0, -1);
    }

    if (cursor_column_on_screen > max_column) {
        m_scroll_position.move_by(0, 1);
    }
}

void Editor::set_mode(Mode m)
{
    if (m_mode == m)
        return;
    m_mode = m;
    m_command = "";
}

static bool is_backspace(int ch)
{
    return ch == 8 || ch == 127;
}

static bool is_newline(int ch)
{
    return ch == 10 || ch == 13;
}

void Editor::handle_command_key_press(int ch)
{
    if (is_backspace(ch)) {
        if (m_command.size() > 0)
            m_command.pop_back();
        else
            set_mode(Idle);
        return;
    }
    if (is_newline(ch)) {
        if (m_command.size() > 0)
            exec_command();
        set_mode(Idle);
        return;
    }
    m_command.push_back(ch);
}

void Editor::handle_document_key_press(int ch)
{
    if (is_backspace(ch)) {
        //auto op = make<EraseOperation>(1);
        m_document->backspace_at(m_cursor);
    } else {
        auto op = make<InsertOperation>(ch);
        run(std::move(op));
    }
}

void Editor::run(OwnPtr<Operation>&& op)
{
    ASSERT(op);
    op->apply(*this);
    m_undo_stack.push(std::move(op));
}

void Editor::insert_at_cursor(int ch)
{
    std::string s;
    s += ch;
    m_document->insert_at(m_cursor, s);
    m_cursor.move_by(0, 1);
}

bool Editor::insert_text_at_cursor(const std::string& text)
{
    ASSERT(text.size() == 1);
    if (text[0] == '\n') {
        m_document->newline_at(m_cursor);
        m_cursor.move_to(m_cursor.line() + 1, 0);
        return true;
    }
    m_document->insert_at(m_cursor, text);
    m_cursor.move_by(0, text.size());
    return true;
}

bool Editor::remove_text_at_cursor(const std::string& text)
{
    // FIXME: Implement
    ASSERT(false);
    return false;
}

void Editor::erase_left()
{
    if (m_cursor.column() == 0)
        return;
    m_document->erase_at(m_cursor, -1);
    m_cursor.move_by(0, -1);
}

Line& Editor::current_line()
{
    return m_document->line(m_cursor.line());
}

const Line& Editor::current_line() const
{
    return m_document->line(m_cursor.line());
}

void Editor::erase_right()
{
    if (m_cursor.column() == current_line().length())
        return;
    m_document->erase_at(m_cursor, 1);
}

void Editor::set_status_text(const std::string& text)
{
    m_status_text = text;
}

void Editor::set_status_text(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    char buf[128];
    vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    m_status_text = buf;
}

void Editor::exec_command()
{
    if (m_command == "q") {
        m_should_quit = true;
        return;
    }

    if (m_command == "w") {
        write_to_file();
        return;
    }

    if (m_command == "about") {
        set_status_text("cuki editor!");
        return;
    }

    std::string s;
    s = "Invalid command: '";
    s += m_command;
    s += "'";
    set_status_text(s);
}
