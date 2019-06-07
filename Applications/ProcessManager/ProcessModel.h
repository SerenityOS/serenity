#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibCore/CFile.h>
#include <LibGUI/GModel.h>
#include <unistd.h>

class GraphWidget;

class ProcessModel final : public GModel {
public:
    enum Column
    {
        Icon = 0,
        Name,
        CPU,
        State,
        Priority,
        User,
        PID,
        Virtual,
        Physical,
        Syscalls,
        __Count
    };

    static Retained<ProcessModel> create(GraphWidget& graph) { return adopt(*new ProcessModel(graph)); }
    virtual ~ProcessModel() override;

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit ProcessModel(GraphWidget&);

    GraphWidget& m_graph;

    struct ProcessState {
        pid_t pid;
        unsigned nsched;
        String name;
        String state;
        String user;
        String priority;
        size_t virtual_size;
        size_t physical_size;
        unsigned syscalls;
        float cpu_percent;
    };

    struct Process {
        ProcessState current_state;
        ProcessState previous_state;
    };

    HashMap<uid_t, String> m_usernames;
    HashMap<pid_t, OwnPtr<Process>> m_processes;
    Vector<pid_t> m_pids;
    RetainPtr<GraphicsBitmap> m_generic_process_icon;
    RetainPtr<GraphicsBitmap> m_high_priority_icon;
    RetainPtr<GraphicsBitmap> m_low_priority_icon;
    RetainPtr<GraphicsBitmap> m_normal_priority_icon;
    CFile m_proc_all;
};
