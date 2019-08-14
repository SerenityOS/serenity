#pragma once

#include <AK/Function.h>
#include <LibGUI/GTableView.h>
#include <unistd.h>

class GraphWidget;
class ProcessModel;

class ProcessTableView final : public GTableView {
public:
    ProcessTableView(GraphWidget&, GWidget* parent);
    virtual ~ProcessTableView() override;

    pid_t selected_pid() const;

    void refresh();

    Function<void(pid_t)> on_process_selected;

private:
    virtual void model_notification(const GModelNotification&) override;
};
