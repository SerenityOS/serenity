#pragma once

#include <AK/AKString.h>
#include <LibHTML/ParentNode.h>

class Document : public ParentNode {
public:
    Document();
    virtual ~Document() override;

private:
};

