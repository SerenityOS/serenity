#pragma once

#include <AK/JsonArray.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/CLocalSocket.h>
#include <LibGUI/GModel.h>

class RemoteObjectGraphModel final : public GModel {
public:
    static NonnullRefPtr<RemoteObjectGraphModel> create_with_pid(pid_t pid)
    {
        return adopt(*new RemoteObjectGraphModel(pid));
    }

    virtual ~RemoteObjectGraphModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual GModelIndex index(int row, int column, const GModelIndex& parent = GModelIndex()) const override;
    virtual void update() override;

private:
    struct RemoteObject {
        RemoteObject* parent { nullptr };
        Vector<OwnPtr<RemoteObject>> children;

        String address;
        String parent_address;
        String class_name;
        String name;
    };

    explicit RemoteObjectGraphModel(pid_t);

    pid_t m_pid { -1 };
    CLocalSocket m_socket;
    JsonArray m_json;
    NonnullOwnPtrVector<RemoteObject> m_remote_roots;
    GIcon m_object_icon;
};
