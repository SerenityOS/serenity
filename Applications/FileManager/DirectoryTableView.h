#pragma once

#include <LibGUI/GTableView.h>
#include <sys/stat.h>
#include "DirectoryTableModel.h"

class DirectoryTableView final : public GTableView {
public:
    explicit DirectoryTableView(GWidget* parent);
    virtual ~DirectoryTableView() override;

    void open(const String& path);
    String path() const { return model().path(); }

    Function<void(const String&)> on_path_change;
    Function<void(String)> on_status_message;

private:
    virtual void model_notification(const GModelNotification&) override;

    DirectoryTableModel& model() { return static_cast<DirectoryTableModel&>(*GTableView::model()); }
    const DirectoryTableModel& model() const { return static_cast<const DirectoryTableModel&>(*GTableView::model()); }

    void set_status_message(const String&);
};
