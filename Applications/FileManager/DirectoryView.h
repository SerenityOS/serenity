#pragma once

#include <AK/Vector.h>
#include <LibGUI/GDirectoryModel.h>
#include <LibGUI/GItemView.h>
#include <LibGUI/GStackWidget.h>
#include <LibGUI/GTableView.h>
#include <sys/stat.h>

class DirectoryView final : public GStackWidget {
public:
    explicit DirectoryView(GWidget* parent);
    virtual ~DirectoryView() override;

    void open(const StringView& path);
    String path() const { return model().path(); }
    void open_parent_directory();
    void open_previous_directory();
    void open_next_directory();
    int path_history_size() const { return m_path_history.size(); }
    int path_history_position() const { return m_path_history_position; }

    void refresh();

    Function<void(const StringView&)> on_path_change;
    Function<void(GAbstractView&)> on_selection;
    Function<void(const StringView&)> on_status_message;
    Function<void(int done, int total)> on_thumbnail_progress;

    enum ViewMode {
        Invalid,
        List,
        Icon
    };
    void set_view_mode(ViewMode);
    ViewMode view_mode() const { return m_view_mode; }

    const GAbstractView& current_view() const
    {
        switch (m_view_mode) {
        case ViewMode::List:
            return *m_table_view;
        case ViewMode::Icon:
            return *m_item_view;
        default:
            ASSERT_NOT_REACHED();
        }
    }

    template<typename Callback>
    void for_each_view_implementation(Callback callback)
    {
        callback(*m_table_view);
        callback(*m_item_view);
    }

private:
    GDirectoryModel& model() { return *m_model; }
    const GDirectoryModel& model() const { return *m_model; }

    void handle_activation(const GModelIndex&);

    void set_status_message(const StringView&);

    ViewMode m_view_mode { Invalid };

    NonnullRefPtr<GDirectoryModel> m_model;
    int m_path_history_position { 0 };
    Vector<String> m_path_history;
    void add_path_to_history(const StringView& path);

    GTableView* m_table_view { nullptr };
    GItemView* m_item_view { nullptr };
};
