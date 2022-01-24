#
# Options specific to the Serenity (target) build
#

include(${CMAKE_CURRENT_LIST_DIR}/common_options.cmake)

serenity_option(ENABLE_PCI_IDS_DOWNLOAD ON CACHE BOOL "Enable download of the pci.ids database at build time")
serenity_option(ENABLE_USB_IDS_DOWNLOAD ON CACHE BOOL "Enable download of the usb.ids database at build time")
serenity_option(ENABLE_PNP_IDS_DOWNLOAD ON CACHE BOOL "Enable download of the pnp.ids database at build time")
serenity_option(ENABLE_KERNEL_ADDRESS_SANITIZER OFF CACHE BOOL "Enable kernel address sanitizer testing in gcc/clang")
serenity_option(ENABLE_KERNEL_COVERAGE_COLLECTION  OFF CACHE BOOL "Enable KCOV and kernel coverage instrumentation in gcc/clang")
serenity_option(ENABLE_KERNEL_LTO OFF CACHE BOOL "Build the kernel with link-time optimization")
serenity_option(ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS  OFF CACHE BOOL "Enable -Og and -ggdb3 options for Kernel code for easier debugging")
serenity_option(ENABLE_MOLD_LINKER OFF CACHE BOOL "Link the SerenityOS userland with the mold linker")
