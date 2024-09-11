## Name

postcreate - HackStudio postcreate scripts

## Synopsis

.postcreate shell scripts are executed by HackStudio after creating a new project.

## Description

It is possible to define project templates that set up HackStudio projects. These templates can contain custom setup logic in the form of a `*.postcreate` script in the template directory. The script name must match the template's (directory) name. Postcreate scripts are regular shell scripts. They are executed from an indeterminate directory with the following arguments:

-   The path to the postcreate script
-   The name of the new project
-   The path of the new project, i.e. not the parent directory it was created in
-   The project name in a form that is safe for C++ namespaces

The script may error out with a non-zero exit code, but the project is still created. The user is informed of the error.

## See Also

-   `ProjectTemplate::create_project` in [`Userland/DevTools/HackStudio/ProjectTemplate.cpp`](../../../../../Userland/DevTools/HackStudio/ProjectTemplate.cpp).
