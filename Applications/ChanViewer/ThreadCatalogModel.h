#pragma once

#include <AK/JsonArray.h>
#include <LibCore/CHttpJob.h>
#include <LibGUI/GModel.h>

class ThreadCatalogModel final : public GModel {
public:
    enum Column {
        ThreadNumber,
        Subject,
        Text,
        ReplyCount,
        ImageCount,
        PostTime,
        __Count,
    };

    static NonnullRefPtr<ThreadCatalogModel> create() { return adopt(*new ThreadCatalogModel); }
    virtual ~ThreadCatalogModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    const String& board() const { return m_board; }
    void set_board(const String&);

    Function<void()> on_load_started;
    Function<void(bool success)> on_load_finished;

private:
    ThreadCatalogModel();

    String m_board { "g" };
    JsonArray m_catalog;
    ObjectPtr<CHttpJob> m_pending_job;
};
