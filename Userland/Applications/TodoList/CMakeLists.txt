serenity_component(
    TodoList
    TARGETS TodoList
)

set(SOURCES
    main.cpp
    TodoList.cpp
    Task.cpp
    TaskListModel.cpp
)

serenity_app(TodoList ICON app-todolist)
target_link_libraries(TodoList LibGUI)
