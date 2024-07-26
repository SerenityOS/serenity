serenity_component(
    Shell
    REQUIRED
    TARGETS Shell
)

if (ENABLE_FUZZERS)
    add_custom_target(Shell) # Stub target to placate serenity_component
else()
    set(SOURCES main.cpp)

    serenity_bin(Shell)
    target_link_libraries(Shell PRIVATE LibCore LibShell LibFileSystem LibLine LibMain)
endif()
