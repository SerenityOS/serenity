#pragma once

#include <LibGUI/GTableView.h>
#include <AK/Function.h>
#include <unistd.h>

class ProcessTableModel;

class ProcessTableView final : public GTableView {
public:
    explicit ProcessTableView(GWidget* parent);
    virtual ~ProcessTableView() override;

    pid_t selected_pid() const;

    Function<void(String)> on_status_message;

protected:
    virtual void model_notification(const GModelNotification&) override;

private:
    virtual void timer_event(GTimerEvent&) override;

    ProcessTableModel& model();
    const ProcessTableModel& model() const;
};

