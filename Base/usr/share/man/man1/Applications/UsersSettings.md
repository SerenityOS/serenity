## Name

![Icon](/res/icons/16x16/app-user-settings.png) Users Settings

[Open](launch:///bin/UsersSettings)

## Synopsis

```**sh
$ UsersSettings
```

## Description

`Users Settings` is an application for managing user accounts on the system.

### User List

The left panel lists all user accounts present on the system. Selecting a user displays their details in the right panel.

### Adding a User

Click the **Add** button below the user list to open the _Add User_ dialog. Fill in:

-   **Account Type** — _Standard_ for a regular user or _Administrator_ to also grant membership of the `wheel` group.
-   **Full Name** — The user's display name (optional).
-   **Username** — The login name for the new account.
-   **Password** — The initial password for the account.

New users are automatically added to the `users`, `window`, `audio`, `lookup` and `phys` groups, so they can login to their own desktop environment.

### Editing a User

Select a user from the list to view and edit their details:

-   **Full Name** — The user's display name stored in the GECOS field.
-   **Shell** — User's login shell.
-   **Account Type** — _Standard_ or _Administrator_. Changing this adds or removes the user from the `wheel` group.

Click **Apply Changes** to save any modifications.

### Changing a Password

Click the **Change Password...** button in the user details panel to open the _Change Password_ dialog. Enter and confirm the new password, then click **OK**.

### Deleting a User

Select a user from the list and click the **Delete** button. A confirmation dialog will appear before the account and its home directory are permanently removed.
