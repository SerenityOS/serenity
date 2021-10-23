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

* **`ahci_reset_mode`** - This parameter expects one of the following values. **`controller`** - Reset just the AHCI controller on boot.
   **`none`** - Don't perform any AHCI reset.  **`complete`** - Reset the AHCI controller, and all AHCI ports on boot.

* **`boot_prof`** - If present on the command line, global system profiling will be enabled
   as soon as possible during the boot sequence. Allowing you to profile startup of all applications.

* **`disable_ide`** - If present on the command line, the IDE controller will not be initialized
   during the boot sequence. Leaving only the AHCI and Ram Disk controllers.

* **`disable_physical_storage`** - If present on the command line, neither AHCI, or IDE controllers will be initialized on boot.
  
* **`disable_ps2_controller`** - If present on the command line, the I8042 keyboard controller will not be initialized on boot.
  
* **`disable_uhci_controller`** - If present on the command line, the UHCI controller will not be initialized on boot.

* **`disable_virtio`** - If present on the command line, virtio devices will not be detected, and initialized on boot.

* **`fbdev`** - This parameter expects **`on`** or **`off`**.

* **`force_pio`** - If present on the command line, the IDE controllers will be force into PIO mode when initialized IDE Channels on boot.

* **`hpet`** - This parameter expects one of the following values. **`periodic`** - The High Precision Event Timer should
  be configured in a periodic mode. **`nonperiodic`** - The High Precision Event Timer should eb configure din non-periodic mode.

* **`init`** - This parameter expects the fully qualified path to the init program the Kernel should launch after boot.
    This defaults to [`SystemServer`(7)](../man7/SystemServer.md).

* **`init_args`** - This parameter expects a set of arguments to pass to the **`init`** program.
  The value should be a set of strings separated by `,` characters.

* **`panic`** - This parameter expects **`halt`** or **`shutdown`**. This is particularly useful in CI contexts.

* **`pci_ecam`** - This parameter expects **`on`** or **`off`**.

* **`root`** - This parameter configures the device to use as the root file system. It defaults to **`/dev/hda`** if unspecified.
  
* **`smp`** - This parameter expects a binary value of **`on`** or **`off`**. If enabled kernel will
  use [APIC](https://en.wikipedia.org/wiki/Advanced_Programmable_Interrupt_Controller) mode
  for handling interrupts instead of [PIC](https://en.wikipedia.org/wiki/Programmable_interrupt_controller) mode.
  This parameter defaults to **`off`**.

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

* [`SystemServer`(7)](../man7/SystemServer.md).
