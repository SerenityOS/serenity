#include <AK/Function.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Arch/Processor.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/Processor.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/Processor.h>
#endif

namespace Kernel {

#if ARCH(X86_64) || ARCH(I386)
ProcessorImpl* Processor::impl { &x86Processor::current() };
#elif ARCH(AARCH64)
ProcessorImpl* Processor::impl { &aarch64Processor::current() };
#else
#    error "Processor didn't rekognice the architecture"
#endif

ProcessorImpl& Processor::current()
{
    return *impl;
}

void Processor::early_initialize(u32 cpu)
{
    impl->early_initialize(cpu);
}

void Processor::initialize(u32 cpu)
{
    impl->initialize(cpu);
}

void Processor::wait_check()
{
    impl->wait_check();
}

Thread* Processor::current_thread()
{
    return impl->current_thread();
}

Thread* Processor::idle_thread()
{
    return impl->idle_thread();
}

FlatPtr Processor::current_in_irq()
{
    return impl->current_in_irq();
}

u32 Processor::in_critical()
{
    return impl->in_critical();
}

bool Processor::has_nx()
{
    return impl->has_nx();
}

u64 Processor::read_cpu_counter()
{
    return impl->read_cpu_counter();
}

u32 Processor::current_id()
{
    return impl->current_id();
}

void Processor::enter_critical()
{
    impl->enter_critical();
}

void Processor::leave_critical()
{
    impl->leave_critical();
}

StringView Processor::platform_string()
{
    return impl->platform_string();
}

[[noreturn]] void Processor::assume_context(Thread& thread, FlatPtr flags)
{
    impl->assume_context(thread, flags);
}

u32 Processor::count()
{
    return impl->count();
}

[[noreturn]] void Processor::halt()
{
    impl->halt();
}

Descriptor& Processor::get_gdt_entry(u16 selector)
{
    return impl->get_gdt_entry(selector);
}

DescriptorTablePointer const& Processor::get_gdtr()
{
    return impl->get_gdtr();
}

void Processor::deferred_call_queue(Function<void()> callback)
{
    impl->deferred_call_queue(move(callback));
}

void Processor::smp_enable()
{
    impl->smp_enable();
}

u32 Processor::smp_wake_n_idle_processors(u32 wake_count)
{
    return impl->smp_wake_n_idle_processors(wake_count);
}

ProcessorInfo& Processor::info()
{
    return impl->info();
}

void Processor::flush_entire_tlb_local()
{
    impl->flush_entire_tlb_local();
}

ProcessorImpl& Processor::by_id(u32 id)
{
    return impl->by_id(id);
}

bool Processor::is_bootstrap_processor()
{
    return impl->is_bootstrap_processor();
}

u8 Processor::physical_address_bit_width()
{
    return impl->physical_address_bit_width();
}

bool Processor::has_pat()
{
    return impl->has_pat();
}

bool Processor::has_rdseed()
{
    return impl->has_feature(CPUFeature::RDSEED);
}

bool Processor::has_rdrand()
{
    return impl->has_feature(CPUFeature::RDRAND);
}

bool Processor::has_tsc()
{
    return impl->has_feature(CPUFeature::TSC);
}

bool Processor::has_smap()
{
    return impl->has_feature(CPUFeature::SMAP);
}

bool Processor::has_hypervisor()
{
    return impl->has_feature(CPUFeature::HYPERVISOR);
}

bool Processor::is_initialized()
{
    return impl->is_initialized();
}

u32 Processor::clear_critical()
{
    return impl->clear_critical();
}

void Processor::restore_critical(u32 prev_critical)
{
    impl->restore_critical(prev_critical);
}

FPUState const& Processor::clean_fpu_state()
{
    return impl->clean_fpu_state();
}

bool Processor::current_in_scheduler()
{
    return impl->current_in_scheduler();
}
void Processor::set_current_in_scheduler(bool value)
{
    impl->set_current_in_scheduler(value);
}

void Processor::flush_tlb_local(VirtualAddress vaddr, size_t page_count)
{
    impl->flush_tlb_local(vaddr, page_count);
}

void Processor::flush_tlb(Memory::PageDirectory const* pdir, VirtualAddress vaddr, size_t page_count)
{
    impl->flush_tlb(pdir, vaddr, page_count);
}

ErrorOr<AK::Vector<FlatPtr, 32>> Processor::capture_stack_trace(Thread& thread, size_t max_frames)
{
    return impl->capture_stack_trace(thread, max_frames);
}

void Processor::set_current_thread(Thread& current_thread)
{
    impl->set_current_thread(current_thread);
}

void Processor::set_specific(ProcessorSpecificDataID specific_id, void* ptr)
{
    impl->set_specific(specific_id, ptr);
}

template<typename T>
T* Processor::get_specific()
{
    return impl->get_specific<T>();
}

template<IteratorFunction<ProcessorImpl&> Callback>
IterationDecision Processor::for_each(Callback callback)
{
    return impl->for_each(move(callback));
}

template<VoidFunction<ProcessorImpl&> Callback>
IterationDecision Processor::for_each(Callback callback)
{
    return impl->for_each(move(callback));
}

ErrorOr<void> Processor::try_for_each(Function<ErrorOr<void>(ProcessorImpl&)> callback)
{
    return impl->try_for_each(move(callback));
}

namespace Memory {
class MemoryManagerData;
}
template Memory::MemoryManagerData* Processor::get_specific<Memory::MemoryManagerData>();

/*
template<typename T>
void ProcessorSpecific<T>::initialize()
{
Processor::current().set_specific(T::processor_specific_data_id(), new T);
}

template<typename T>
T& ProcessorSpecific<T>::get()
{
return *Processor::current().get_specific<T>();
}
*/
}
