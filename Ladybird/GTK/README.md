# Ladybird: GTK 4 version

Well hello friends! :^)

This is a WIP attempt to develop an elegant, beautiful UI for the SerenityOS Browser engine, based on GTK 4 and Adwaita.

This is a project by us, for us, based on the things we like. If you like some of the same things, you are welcome to join the project!

## License

This project is licensed under a 2-clause BSD license.

## Building

The easiest way to build is to use `flatpak-builder` to create a flatpak dev environment.


### Setup: Install flatpak and dependencies


On a Debian-based system like Ubuntu, the flatpak installation steps will look like below:

```sh
# Install flatpak if it's not already
sudo apt install flatpak flatpak-builder
```

You will likely need to re-log your user session after installing flatpak to setup the environment variables properly.
To verify that ``XDG_DATA_DIRS`` is setup properly, run ``env | grep XDG``. Something similar to the output below should be present.

```
XDG_DATA_DIRS=$HOME/.local/share/flatpak/exports/share:/var/lib/flatpak/exports/share:<os-defaults>
```

### Setup: Install GNOME development flatpak packages

Once flatpak is installed on your system, install the GNOME Platform and Sdk packages from the gnome-nightly repository.
The project requires unreleased libadwaita-1 features.

```sh
# Make sure that the gnome-nightly repositories are in your user install directory
flatpak remote-add --user --if-not-exists gnome-nightly https://nightly.gnome.org/gnome-nightly.flatpakrepo

# Install the GNOME SDK
flatpak install --user org.gnome.Platform//master org.gnome.Sdk//master
```

### Build: Build the application package

```sh
# Build and install the application to your local flatpak install
flatpak-builder --user --install --force-clean --ccache build org.serenityos.Ladybird-gtk4.json 

# Run the app
flatpak run --user org.serenityos.Ladybird-gtk4
```

If you are only modifying the ladybird-gtk4 files, then adding the `--keep-build-dirs` flag will help
reduce churn for the serenity components. The `--ccache` flag is essentially mandatory to reduce incremental build times.
