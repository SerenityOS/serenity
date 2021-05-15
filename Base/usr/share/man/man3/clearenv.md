## Name

clearenv - clear the environment

## Synopsis

```**c++
#include <stdlib.h>

clearenv();
```

## Description

Clears all environment variables and sets the external
variable `environ` to NULL.

## Return value

The `clearenv()` function returns zero.

## Examples

```c++
#include <stdlib.h>

int main()
{
    clearenv();
    putenv("PATH=/bin");

    return 0;
}
```
