# SerenityOS RISC-V Port

SerenityOS supports the [RISC-V](https://riscv.org/) ISA.
Only systems with the RV64I base ISA are supported (so 64-bit only).

You can run SerenityOS in the RISC-V QEMU 'virt' machine with:

```console
Meta/serenity.sh run riscv64
```

SMP (multi-core) is currently not supported.
Only the boot hart (CPU) will be used.

Booting via UEFI is supported if the firmware provides a devicetree configuration table.
ACPI is currently not supported.

## Requirements

SerenityOS currently requires the following extensions next to the RV64I base ISA:

-   **A**: Atomic Instructions
-   **F**: Single-Precision Floating-Point
-   **D**: Double-Precision Floating-Point
-   **Zicntr**: Base Counters and Timers
-   **Zicsr**: Control and Status Register (CSR) Instructions
-   **Zifencei**: Instruction-Fetch Fence
-   **Ss1p13**: Supervisor Architecture v1.13

The kernel requires an [SBI](https://github.com/riscv-non-isa/riscv-sbi-doc) 0.2 or later implementation, such as [OpenSBI](https://github.com/riscv-software-src/opensbi).
The SBI implementation needs to support the timer extension "TIME" if the Sstc extension isn't present.

By default, SerenityOS is compiled for RV64GC (RV64IMAFDCZicsr_Zifencei).

The following extensions defined by the [RISC-V Profiles](https://github.com/riscv/riscv-profiles) are required:

-   **Ziccamoa**: Main memory supports all atomics in A
-   **Ziccif**: Main memory supports instruction fetch with atomicity requirement
-   **Ziccrse**: Main memory supports forward progress on LR/SC sequences
-   **Ssccptr**: Main memory supports page table reads
-   **Sstvala**: `stval` provides all needed values
-   **Sstvecd**: `stvec` supports Direct mode
-   **Ssu64xl**: UXLEN=64 must be supported
-   **Svbare**: Bare mode virtual-memory translation supported (only when not booting via UEFI)

The following extensions are used by the kernel, if supported:

-   **V**: Vector Operations (context switching of V registers is supported)
-   **Zihintpause**: Pause Hint
-   **Sstc**: Supervisor-mode Timer Interrupts
