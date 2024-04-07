serenity_component(
    SystemServer
    REQUIRED
    TARGETS SystemServer
)

set(SOURCES
    main.cpp
    Service.cpp
)

serenity_bin(SystemServer)
target_link_libraries(SystemServer PRIVATE LibCore LibFileSystem LibMain)

# Required for conditionally creating hardcoded /dev/kcov entry
if (ENABLE_KERNEL_COVERAGE_COLLECTION)
    add_compile_definitions(ENABLE_KERNEL_COVERAGE_COLLECTION)
endif()
