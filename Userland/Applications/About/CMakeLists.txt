serenity_component(
    About
    REQUIRED
    TARGETS About
)

set(SOURCES
    main.cpp
)

execute_process(COMMAND "git rev-parse --short HEAD" OUTPUT_VARIABLE GIT_COMMIT)
execute_process(COMMAND "git rev-parse --abbrev-ref HEAD" OUTPUT_VARIABLE GIT_BRANCH)
execute_process(COMMAND "git diff-index --quiet HEAD -- && echo tracked || echo untracked" OUTPUT_VARIABLE GIT_CHANGES)

add_definitions(-DGIT_COMMIT="${GIT_COMMIT}" -DGIT_BRANCH="${GIT_BRANCH}" -DGIT_CHANGES="${GIT_CHANGES}")

serenity_app(About ICON ladyball)
target_link_libraries(About PRIVATE LibCore LibGfx LibGUI LibMain)
