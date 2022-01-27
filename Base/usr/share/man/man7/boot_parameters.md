## Name

Boot Parameters - optional parameters accepted by the kernel

## Description

Serenity's kernel can process parameters at boot time.
This functionality is used to control or augment the state of features during the initial
startup of the system.

### Options

The kernel boot parameters take the form of **`{option_name}={value}`**, where the **`={value}`**
trailer can be omitted for specific parameters.

List of options:

* **`acpi`** - This parameter expects one of the following values. **`on`** - Boot with full ACPI support, using ACPI 
   Machine Language interpretation (default). **`limited`** - Boot with limited ACPI support. **`off`** - Don't initialize ACPI at all.

* **`ahci_reset_mode`** - This parameter expects one of the following values. **`controllers`** - Reset just the AHCI controller on boot (default).
   **`aggressive`** - Reset the AHCI controller, and all AHCI ports on boot.

* **`boot_prof`** - If present on the command line, global system profiling will be enabled
   as soon as possible during the boot sequence. Allowing you to profile startup of all applications.

* **`disable_ide`** - If present on the command line, the IDE controller will not be initialized
   during the boot sequence. Leaving only the AHCI and Ram Disk controllers.

* **`disable_physical_storage`** - If present on the command line, neither AHCI, or IDE controllers will be initialized on boot.
  
* **`disable_ps2_controller`** - If present on the command line, the I8042 keyboard controller will not be initialized on boot.
  
* **`disable_uhci_controller`** - If present on the command line, the UHCI controller will not be initialized on boot.

* **`disable_virtio`** - If present on the command line, virtio devices will not be detected, and initialized on boot.

* **`enable_ioapic`** - This parameter expects **`on`** or **`off`** and is by default set to **`on`**.
  When set to **`off`**, the kernel will initialize the two i8259 PICs.
  When set to **`on`**, the kernel will try to initialize the IOAPIC (or IOAPICs if there's more than one),
  but only if **`acpi`** is set to **`limited`** or **`on`**, and a `MADT` (APIC) table is available.
  Otherwise, the kernel will fallback to use the i8259 PICs.

* **`fbdev`** - This parameter expects one of the following values. **`on`**- Boot into the graphical environment (default). **`off`** - Boot into text mode. **`bootloader`** - Boot into the graphical environment, but only use the frame buffer set up by the bootloader and do not initialize any other graphics cards.

* **`force_pio`** - If present on the command line, the IDE controllers will be force into PIO mode when initialized IDE Channels on boot.

* **`hpet`** - This parameter expects one of the following values. **`periodic`** - The High Precision Event Timer should
  be configured in a periodic mode. **`nonperiodic`** - The High Precision Event Timer should eb configure din non-periodic mode.

* **`init`** - This parameter expects the fully qualified path to the init program the Kernel should launch after boot.
    This defaults to [`SystemServer`(7)](help://man/7/SystemServer).

* **`init_args`** - This parameter expects a set of arguments to pass to the **`init`** program.
  The value should be a set of strings separated by `,` characters.

* **`panic`** - This parameter expects **`halt`** or **`shutdown`**. This is particularly useful in CI contexts.

* **`pci_ecam`** - This parameter expects **`on`** or **`off`**.

* **`root`** - This parameter configures the device to use as the root file system. It defaults to **`/dev/hda`** if unspecified.

* **`pcspeaker`** - This parameter controls whether the kernel can use the PC speaker or not. It defaults to **`off`** and can be set to **`on`** to enable the PC speaker.

* **`smp`** - This parameter expects a binary value of **`on`** or **`off`**. If enabled kernel will
  enable available APs (application processors) and use them with the BSP (Bootstrap processor) to
  schedule and run threads.
  This parameter defaults to **`off`**. This parameter requires **`enable_ioapic`** to be enabled
  and a `MADT` (APIC) table to be available.

* **`nvme_poll`** - This parameter configures the NVMe drive to use polling instead of interrupt driven completion.

* **`system_mode`** - This parameter is not interpreted by the Kernel, and is made available at `/proc/system_mode`. SystemServer uses it to select the set of services that should be started. Common values are:
  - **`graphical`** (default) - Boots the system in the normal graphical mode.
  - **`self-test`** - Boots the system in self-test, validation mode.
  - **`text`** - Boots the system in text only mode. (You may need to also set **`fbdev=off`**.)

* **`time`** - This parameter expects one of the following values. **`modern`** - This configures the system to attempt
  to use High Precision Event Timer (HPET) on boot. **`legacy`** - Configures the system to use the legacy programmable interrupt
  time for managing system team.
  
* **`vmmouse`** - This parameter expects a binary value of **`on`** or **`off`**. If enabled and
  running on a VMWare Hypervisor, the kernel will enable absolute mouse mode.

## See also

* [`SystemServer`(7)](help://man/7/SystemServer).
