#pragma once

#include <LibWeb/Bindings/NodeWrapper.h>

namespace Web {
namespace Bindings {

class DocumentWrapper : public NodeWrapper {
public:
    explicit DocumentWrapper(Document&);
    virtual ~DocumentWrapper() override;

    Document& node();
    const Document& node() const;

private:
    virtual const char* class_name() const override { return "Document"; }
};

}
}
