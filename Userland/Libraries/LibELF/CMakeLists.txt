# LibELF is included within LibC on Serenity and defined as a target in a different place for Lagom,
# so add a dummy interface library for platform-agnosticism.
add_library(LibELF INTERFACE)

serenity_install_headers("LibELF")
serenity_install_sources("Userland/Libraries/LibELF")
