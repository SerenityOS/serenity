#pragma once

#include <LibWeb/Bindings/Wrapper.h>

namespace Web {
namespace Bindings {

class NodeWrapper : public Wrapper {
public:
    explicit NodeWrapper(Node&);
    virtual ~NodeWrapper() override;

    Node& node() { return *m_node; }
    const Node& node() const { return *m_node; }

private:
    virtual const char* class_name() const override { return "Node"; }

    NonnullRefPtr<Node> m_node;
};

}
}
