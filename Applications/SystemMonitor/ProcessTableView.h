#pragma once

#include <AK/Function.h>
#include <LibGUI/GTableView.h>
#include <unistd.h>

class GraphWidget;
class ProcessModel;

class ProcessTableView final : public GTableView {
    C_OBJECT(ProcessTableView)
public:
    virtual ~ProcessTableView() override;

    pid_t selected_pid() const;

    void refresh();

    Function<void(pid_t)> on_process_selected;

private:
    explicit ProcessTableView(GWidget* parent = nullptr);
};
