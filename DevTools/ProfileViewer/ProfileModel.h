#pragma once

#include <LibGUI/GModel.h>

class Profile;

class ProfileModel final : public GModel {
public:
    static NonnullRefPtr<ProfileModel> create(Profile& profile)
    {
        return adopt(*new ProfileModel(profile));
    }

    enum Column {
        SampleCount,
        StackFrame,
        __Count
    };

    virtual ~ProfileModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual GModelIndex index(int row, int column, const GModelIndex& parent = GModelIndex()) const override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual void update() override;
    virtual int tree_column() const override { return Column::StackFrame; }

private:
    explicit ProfileModel(Profile&);

    Profile& m_profile;

    GIcon m_user_frame_icon;
    GIcon m_kernel_frame_icon;
};
