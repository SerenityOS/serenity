#pragma once

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/CLocalSocket.h>
#include <LibGUI/GModel.h>

class RemoteProcess;

class RemoteObjectGraphModel final : public GModel {
public:
    static NonnullRefPtr<RemoteObjectGraphModel> create(RemoteProcess& process)
    {
        return adopt(*new RemoteObjectGraphModel(process));
    }

    virtual ~RemoteObjectGraphModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual GModelIndex index(int row, int column, const GModelIndex& parent = GModelIndex()) const override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual void update() override;

private:
    explicit RemoteObjectGraphModel(RemoteProcess&);

    RemoteProcess& m_process;

    GIcon m_object_icon;
    GIcon m_window_icon;
};
