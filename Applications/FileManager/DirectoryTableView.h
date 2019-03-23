#pragma once

#include <LibGUI/GTableView.h>
#include <sys/stat.h>
#include "DirectoryModel.h"

class DirectoryTableView final : public GTableView {
public:
    explicit DirectoryTableView(GWidget* parent);
    virtual ~DirectoryTableView() override;

    void open(const String& path);
    String path() const { return model().path(); }
    void open_parent_directory();

    void refresh();

    Function<void(const String&)> on_path_change;
    Function<void(String)> on_status_message;

private:
    virtual void model_notification(const GModelNotification&) override;

    DirectoryModel& model() { return *m_model; }
    const DirectoryModel& model() const { return *m_model; }

    void set_status_message(const String&);

    Retained<DirectoryModel> m_model;
};
