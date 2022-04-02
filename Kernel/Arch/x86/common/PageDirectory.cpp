#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Thread.h>

namespace Kernel::Memory {

    void activate_kernel_page_directory(PageDirectory const& pgd)
    {       
        write_cr3(pgd.cr3());
    }

    void activate_page_directory(PageDirectory const& pgd, Thread *current_thread)
    {       
        current_thread->regs().cr3 = pgd.cr3();
        write_cr3(pgd.cr3());
    }

}
