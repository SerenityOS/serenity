#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <AK/FileSystemPath.h>
#include <AK/HashMap.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTableModel.h>
#include "ProcessView.h"

static HashMap<unsigned, String>* s_usernames;

class ProcessTableModel final : public GTableModel {
public:
    ProcessTableModel()
    {

    }
    virtual ~ProcessTableModel() override { }

    virtual int row_count() const override { return m_processes.size(); }
    virtual int column_count() const override { return 3; }

    virtual String column_name(int column) const override
    {
        switch (column) {
        case 0: return "PID";
        case 1: return "State";
        case 2: return "Name";
        default: ASSERT_NOT_REACHED();
        }
    }
    virtual int column_width(int column) const override
    {
        switch (column) {
        case 0: return 30;
        case 1: return 80;
        case 2: return 100;
        default: ASSERT_NOT_REACHED();
        }
    }

    virtual GModelIndex selected_index() const override { return { m_selected_row, 0 }; }
    virtual void set_selected_index(GModelIndex index) override
    {
        if (index.row() >= 0 && index.row() < m_pids.size())
            m_selected_row = index.row();
    }

    virtual String data(int row, int column) const override
    {
        if (row < 0 || row >= row_count())
            return { };
        if (column < 0 || column >= column_count())
            return { };
        auto it = m_processes.find(m_pids[row]);
        auto& process = *(*it).value;
        switch (column) {
        case 0: return String::format("%d", process.current_state.pid);
        case 1: return process.current_state.state;
        case 2: return process.current_state.name;
        }
        ASSERT_NOT_REACHED();
    }

    virtual void update() override
    {
        FILE* fp = fopen("/proc/all", "r");
        if (!fp) {
            perror("failed to open /proc/all");
            exit(1);
        }

        HashTable<pid_t> live_pids;
        for (;;) {
            char buf[BUFSIZ];
            char* ptr = fgets(buf, sizeof(buf), fp);
            if (!ptr)
                break;
            auto parts = String(buf, Chomp).split(',');
            if (parts.size() < 17)
                break;
            bool ok;
            pid_t pid = parts[0].to_uint(ok);
            ASSERT(ok);
            unsigned nsched = parts[1].to_uint(ok);
            ASSERT(ok);
            ProcessState state;
            state.pid = pid;
            state.nsched = nsched;
            unsigned uid = parts[5].to_uint(ok);
            ASSERT(ok);
            //state.user = s_usernames->get(uid);
            state.user = String::format("%u", uid);
            state.priority = parts[16];
            state.state = parts[7];
            state.name = parts[11];
            state.linear = parts[12].to_uint(ok);
            ASSERT(ok);
            state.committed = parts[13].to_uint(ok);
            ASSERT(ok);

            {
                auto it = m_processes.find(pid);
                if (it == m_processes.end())
                    m_processes.set(pid, make<Process>());
            }
            auto it = m_processes.find(pid);
            ASSERT(it != m_processes.end());
            (*it).value->previous_state = (*it).value->current_state;
            (*it).value->current_state = state;

            live_pids.set(pid);
        }
        int rc = fclose(fp);
        ASSERT(rc == 0);

        m_pids.clear();
        Vector<pid_t> pids_to_remove;
        for (auto& it : m_processes) {
            if (!live_pids.contains(it.key)) {
                pids_to_remove.append(it.key);
                continue;
            }
            m_pids.append(it.key);
        }
        for (auto pid : pids_to_remove)
            m_processes.remove(pid);
    }

    pid_t selected_pid() const
    {
        if (m_selected_row == -1)
            return -1;
        return m_pids[m_selected_row];
    }

private:
    struct ProcessState {
        pid_t pid;
        unsigned nsched;
        String name;
        String state;
        String user;
        String priority;
        unsigned linear;
        unsigned committed;
        unsigned nsched_since_prev;
        float cpu_percent;
    };

    struct Process {
        ProcessState current_state;
        ProcessState previous_state;
    };

    HashMap<pid_t, OwnPtr<Process>> m_processes;
    Vector<pid_t> m_pids;
    int m_selected_row { -1 };
};

ProcessView::ProcessView(GWidget* parent)
    : GWidget(parent)
{
    m_process_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/gear16.rgb", { 16, 16 });

    m_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_scrollbar->set_step(4);
    m_scrollbar->set_big_step(30);
    m_scrollbar->on_change = [this] (int) {
        update();
    };

    m_model = make<ProcessTableModel>();

    start_timer(1000);
    reload();
}

ProcessView::~ProcessView()
{
}

void ProcessView::timer_event(GTimerEvent&)
{
    reload();
}

void ProcessView::resize_event(GResizeEvent& event)
{
    m_scrollbar->set_relative_rect(event.size().width() - m_scrollbar->preferred_size().width(), 0, m_scrollbar->preferred_size().width(), event.size().height());
}

void ProcessView::reload()
{
    m_model->update();

    int excess_height = max(0, (item_count() * item_height()) - height());
    m_scrollbar->set_range(0, excess_height);

    set_status_message(String::format("%d processes", item_count()));
    update();
}

Rect ProcessView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), width(), item_height() };
}

void ProcessView::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        for (int i = 0; i < item_count(); ++i) {
            if (!row_rect(i).contains(event.position()))
                continue;
            m_model->set_selected_index({ i, 0 });
            update();
        }
    }
}

void ProcessView::paint_event(GPaintEvent&)
{
    Painter painter(*this);

    int horizontal_padding = 5;
    int painted_item_index = 0;

    int x_offset = 0;
    for (int column_index = 0; column_index < m_model->column_count(); ++column_index) {
        Rect cell_rect(horizontal_padding + x_offset, 0, m_model->column_width(column_index), item_height());
        painter.draw_text(cell_rect, m_model->column_name(column_index), TextAlignment::CenterLeft, Color::Black);
        x_offset += m_model->column_width(column_index) + horizontal_padding;
    }
    painter.draw_line({ 0, 0 }, { width() - 1, 0 }, Color::White);
    painter.draw_line({ 0, header_height() - 1 }, { width() - 1, header_height() - 1 }, Color::DarkGray);

    int y_offset = header_height();

    for (int row_index = 0; row_index < m_model->row_count(); ++row_index) {
        int y = y_offset + painted_item_index * item_height();

        Color background_color;
        Color text_color;
        if (row_index == m_model->selected_index().row()) {
            background_color = Color::from_rgb(0x84351a);
            text_color = Color::White;
        } else {
            background_color = painted_item_index % 2 ? Color(210, 210, 210) : Color::White;
            text_color = Color::Black;
        }

        painter.fill_rect(row_rect(painted_item_index), background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < m_model->column_count(); ++column_index) {
            Rect cell_rect(horizontal_padding + x_offset, y, m_model->column_width(column_index), item_height());
            painter.draw_text(cell_rect, m_model->data(row_index, column_index), TextAlignment::CenterLeft, text_color);
            x_offset += m_model->column_width(column_index) + horizontal_padding;
        }
        ++painted_item_index;
    };

    Rect unpainted_rect(0, painted_item_index * item_height(), width(), height());
    unpainted_rect.intersect(rect());
    painter.fill_rect(unpainted_rect, Color::White);
}

void ProcessView::set_status_message(String&& message)
{
    if (on_status_message)
        on_status_message(move(message));
}

int ProcessView::item_count() const
{
    return m_model->row_count();
}

pid_t ProcessView::selected_pid() const
{
    return m_model->selected_pid();
}
