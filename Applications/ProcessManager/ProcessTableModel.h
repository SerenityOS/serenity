#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibGUI/GTableModel.h>
#include <unistd.h>

class ProcessTableModel final : public GTableModel {
public:
    enum Column {
        Icon = 0,
        Name,
        CPU,
        State,
        Priority,
        User,
        PID,
        Linear,
        Physical,
        __Count
    };

    ProcessTableModel();
    virtual ~ProcessTableModel() override;

    virtual int row_count() const override;
    virtual int column_count() const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    pid_t selected_pid() const;

private:
    struct ProcessState {
        pid_t pid;
        unsigned nsched;
        String name;
        String state;
        String user;
        String priority;
        size_t linear;
        size_t physical;
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
};
