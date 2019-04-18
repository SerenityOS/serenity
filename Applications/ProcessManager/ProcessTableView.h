#pragma once

#include <LibGUI/GTableView.h>
#include <AK/Function.h>
#include <unistd.h>

class ProcessModel;

class ProcessTableView final : public GTableView {
public:
    explicit ProcessTableView(GWidget* parent);
    virtual ~ProcessTableView() override;

    pid_t selected_pid() const;

    void refresh();

private:
    virtual void model_notification(const GModelNotification&) override;
};

