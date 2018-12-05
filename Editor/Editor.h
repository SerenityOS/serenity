#pragma once

#include "OwnPtr.h"
#include "Position.h"
#include "UndoStack.h"
#include <string>

class Document;

class Editor {
public:
    Editor();
    ~Editor();

    void set_document(OwnPtr<Document>&&);
    void redraw();

    int exec();

    enum Mode {
        Idle,
        EditingCommand,
        EditingDocument,
    };

    void set_mode(Mode);
    Mode mode() const { return m_mode; } 
    bool is_editing_document() const { return m_mode == EditingDocument; }
    bool is_editing_command() const { return m_mode == EditingCommand; }
    bool is_idle() const { return m_mode == Idle; }

    void set_status_text(const std::string&);
    void set_status_text(const char* fmt, ...);

    bool insert_text_at_cursor(const std::string&);
    bool remove_text_at_cursor(const std::string&);

    void run(OwnPtr<Operation>&&);

private:
    void move_left();
    void move_down();
    void move_up();
    void move_right();
    void move_to_end_of_line();

    size_t max_line() const;
    size_t max_column() const;

    void update_scroll_position_if_needed();

    void draw_status_bar();
    void draw_cursor();

    void handle_document_key_press(int ch);
    void handle_command_key_press(int ch);

    void insert_at_cursor(int ch);

    void exec_command();
    void coalesce_current_line();
    void write_to_file();

    OwnPtr<Document> m_document;

    UndoStack m_undo_stack;

    // Document relative
    Position m_scroll_position;
    Position m_cursor;

    std::string m_command;
    std::string m_status_text;

    bool m_should_quit { false };
    size_t m_ruler_width { 0 };
    Mode m_mode { Idle };
};
