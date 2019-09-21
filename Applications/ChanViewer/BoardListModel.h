#pragma once

#include <AK/JsonArray.h>
#include <LibCore/CHttpJob.h>
#include <LibGUI/GModel.h>

class BoardListModel final : public GModel {
public:
    enum Column {
        Board,
        __Count,
    };

    static NonnullRefPtr<BoardListModel> create() { return adopt(*new BoardListModel); }
    virtual ~BoardListModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    BoardListModel();

    JsonArray m_boards;
    ObjectPtr<CHttpJob> m_pending_job;
};
