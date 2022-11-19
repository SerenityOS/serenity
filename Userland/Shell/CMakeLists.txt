serenity_component(
    Shell
    REQUIRED
    TARGETS Shell
)

set(SOURCES
    AST.cpp
    Builtin.cpp
    Formatter.cpp
    ImmediateFunctions.cpp
    Job.cpp
    NodeVisitor.cpp
    Parser.cpp
    Shell.cpp
)

serenity_lib(LibShell shell)
target_link_libraries(LibShell PRIVATE LibCore LibLine LibSyntax LibRegex)

serenity_lib_static(LibShellStatic shell)
target_link_libraries(LibShellStatic PRIVATE LibCoreStatic LibLineStatic LibSyntaxStatic LibRegexStatic)

if (SERENITYOS)
    target_sources(LibShell PRIVATE SyntaxHighlighter.cpp)
    target_sources(LibShellStatic PRIVATE SyntaxHighlighter.cpp)
    target_link_libraries(LibShell PRIVATE LibCodeComprehension)
    target_link_libraries(LibShellStatic PRIVATE LibCodeComprehensionStatic)
endif()

if (ENABLE_FUZZERS)
    add_custom_target(Shell) # Stub target to placate serenity_component
else()
    set(SOURCES main.cpp)

    serenity_bin(Shell)
    target_link_libraries(Shell PRIVATE LibCore LibLine LibShell LibMain)

    install(DIRECTORY Tests/ DESTINATION usr/Tests/Shell
            PATTERN "Tests/*"
            PERMISSIONS OWNER_EXECUTE OWNER_WRITE OWNER_READ
            GROUP_EXECUTE GROUP_READ
            WORLD_EXECUTE WORLD_READ)
endif()
