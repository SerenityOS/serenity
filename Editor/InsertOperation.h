#pragma once

#include "Operation.h"
#include <string>

class InsertOperation final : public Operation {
public:
    explicit InsertOperation(const std::string&);
    explicit InsertOperation(char);
    ~InsertOperation();

    virtual bool apply(Editor&) override;
    virtual bool unapply(Editor&) override;

private:
    std::string m_text;
};
