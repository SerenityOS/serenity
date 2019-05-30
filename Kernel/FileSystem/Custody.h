#pragma once

#include <AK/AKString.h>
#include <AK/RetainPtr.h>
#include <AK/Retainable.h>

class Inode;

class Custody : public Retainable<Custody> {
public:
    static Retained<Custody> create(Custody* parent, const String& name, Inode& inode)
    {
        return adopt(*new Custody(parent, name, inode));
    }

    ~Custody();

    Custody* parent() { return m_parent.ptr(); }
    const Custody* parent() const { return m_parent.ptr(); }

    Inode& inode() { return *m_inode; }
    const Inode& inode() const { return *m_inode; }

    const String& name() const { return m_name; }

private:
    Custody(Custody* parent, const String& name, Inode&);

    RetainPtr<Custody> m_parent;
    String m_name;
    Retained<Inode> m_inode;
};
