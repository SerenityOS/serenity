# Ladybird: GTK 4 version

Well hello friends! :^)

This is a WIP attempt to develop an elegant, beautiful UI for the SerenityOS Browser engine, based on GTK 4 and Adwaita.

This is a project by us, for us, based on the things we like. If you like some of the same things, you are welcome to join the project!

## License

This project is licensed under a 2-clause BSD license.

## Building

This project depends on the yet-unreleased libadwaita version. You're either
going to have to build libadwaita from source, or you can use `flatpak-builder`
to create a [Flatpak](https://flatpak.org/) dev environment.


### Setup: Install Flatpak and dependencies

If you don't already have Flatpak installed on your system, install it from
your package manager. On a Debian-based system like Ubuntu, the Flatpak
installation steps will look like below:

```sh
sudo apt install flatpak flatpak-builder
```

You will likely need to re-log into your user session after installing Flatpak
to ensure your environment variables are updated properly. To verify that
`XDG_DATA_DIRS` is set up properly, run `env | grep XDG`. Something similar
to the output below should be present:

```
XDG_DATA_DIRS=$HOME/.local/share/flatpak/exports/share:/var/lib/flatpak/exports/share:<os-defaults>
```

### Setup: Install GNOME development Flatpak packages

Once Flatpak is installed on your system, install the GNOME Platform and SDK
packages from the gnome-nightly repository. You need to use the master branch,
since this project requires unreleased libadwaita features.

```sh
# Make sure that the gnome-nightly repositories are in your user Flatpak installation
flatpak remote-add --user --if-not-exists gnome-nightly https://nightly.gnome.org/gnome-nightly.flatpakrepo

# Install the GNOME SDK
flatpak install --user org.gnome.Platform//master org.gnome.Sdk//master
```

### Build: Build the application package

```sh
# Build and install the application to your local Flatpak installation
flatpak-builder --user --install --force-clean --ccache build org.serenityos.Ladybird-gtk4.json 

# Run the app
flatpak run --user org.serenityos.Ladybird-gtk4
```

If you are only modifying the ladybird-gtk4 files, then adding the `--keep-build-dirs` flag will help
reduce churn for the serenity components. The `--ccache` flag is essentially mandatory to reduce incremental build times.
