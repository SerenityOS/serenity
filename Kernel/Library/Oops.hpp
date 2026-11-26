#pragma once

#include <AK/Types.h>
#include <AK/StringView.h>
#include <AK/Format.h>
#include <Kernel/Integrity/taint.h>
#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {
    enum class OopsCode : u64 {
        VerifyFailed = 1,
        BugOnFailed = 2,
        RequestOopsWriteBytes = 3,
        RequestOopsTruncate = 4
    };

    inline constexpr StringView _get_oopscode_name(const OopsCode code) {
        switch (code) {
            case (OopsCode::VerifyFailed):
                return "VERIFY::FALIED"sv;
            case (OopsCode::BugOnFailed):
                return "BUGON::FAILED"sv;
            case (OopsCode::RequestOopsWriteBytes):
                return "RequestOops::WriteBytes"sv;
            case (OopsCode::RequestOopsTruncate):
                return "RequestOops::Truncate"sv;
        }

        return "UNKNOWN"sv;
    }
}

namespace Kernel {
    template<typename... Args>
    [[noreturn]] void oops(Kernel::OopsCode code, AK::CheckedFormatString<Args...> fmt, Args&&... args) {
        dbgln("----- KERNEL OOPS OCCURED -----");
        dbgln("Code: {} ({})", static_cast<u32>(code), _get_oopscode_name(code));
        dbgln(AK::move(fmt), AK::forward<Args>(args)...);
        dbgln("----- -----");

        Kernel::Integrity::taint.fetch_or(
            static_cast<u64>(TaintFlags::Oops),
            AK::memory_order_seq_cst
        );

        if (Kernel::Process::has_current()) {
            if (!Processor::are_interrupts_enabled())
                PANIC("Kernel oops issued while interrupts are disabled, deemed unrecoverable.");

            auto* proc = &Kernel::Process::current();
            proc->crash(SIGKILL, {});
        } else {
            PANIC("Kernel oops issued outside schedulable context, deemed unrecoverable.");
        }
    };

    [[noreturn]] void oops(Kernel::OopsCode code);
}

#define BUG_ON(expr, ...) \
    do { \
        if (static_cast<bool>(expr)) [[unlikely]] \
            Kernel::oops(Kernel::OopsCode::BugOnFailed __VA_OPT__(,) __VA_ARGS__); \
    } while (0)
