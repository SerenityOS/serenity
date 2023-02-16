## Name

presenter - Presenter slide presentation format (.presenter)

## Description

The presenter file format is a format for specifying slides and presentations in a JSON-based format. It is intentionally simple, allowing it to be written by hand. It is the native format of [Presenter](help://man/1/Applications/Presenter).

This manpage specifies version 1 of the presenter file format.

### Global structure

A presenter file contains a global JSON object with the following three properties:

-   `version`: A number that is incremented every time there is a new version of this format. There are no guarantees that different formats are compatible, and Presenter will only read one version (the mainline repo version will read only the newest one). Please have a look at the history of this specification to see what has changed and how you need to modify the file.
-   `metadata`: An object containing metadata about the presentation. See Metadata below.
-   `templates`: An object containing named layout templates that can be re-used by slide objects. See Templates below.
-   `slides`: An array containing the slides. See Slides below.

### Metadata

Metadata consists of simple key-value pairs of properties, of which any may or may not be present. Metadata by default only contains non-complex JSON types, however this is not required. It is allowed to store other data in the metadata object, as it is not necessary for correct Presenter behavior to read most or any of the properties in this object.

-   `author`: (string) Name of the author of this presentation.
-   `last-modified`: (string containing ISO 8601 date time in UTC) Time when the presentation was last modified. This is useful as file system modification times may often be incorrect, especially when moving files around.
-   `title`: (string) Title of the presentation.
-   `color-scheme`: (string, currently `light` and `dark` are recognized) The color scheme of the presentation. Presenter uses this to determine which color(s) to use for black-out and white-out. Black-out always uses the background color of this color scheme and white-out uses the foreground color. The default is white.
-   `width`: (float) One of the two required metadata properties. Determines the normative width of a slide in abstract units. Together with the aspect ratio, the height obtains a normative size and these two can then be used by slide objects for positioning. Note that because the displayed size of the slide will most likely not match these dimensions, the width is just important for positioning objects, not for determining how large the slide will appear. Dimensions are always translated to the actual displayed size.
-   `aspect-ratio`: (string in the format `width:height`, e.g. `16:9`, width and height are integers) One of the two required metadata properties. Together with the width, determines the normative height of the slide for positioning. This is not the physical size in pixels; see the explanation on `width`.

### Templates

Templates provide a simple way of re-using layout data for multiple slide objects. Templates are very simple and just specify the value of some slide object properties. It is therefore possible to apply multiple templates; the later templates will override values from the previous templates and the slide object properties themselves override any template properties.

Templates are given an ID as their key in the global templates object; this automatically ensures that template IDs are unique. The body of a template object is just the properties this template wishes to set for the slide objects that use it.

### Slides

The slides array contains a list of slide objects, their JSON order determines their order in the presentation. Each slide object contains these properties:

-   `title`: (string, optional) Name of this slide. It may instead be given by a title text, see below.
-   `frames`: (integer, optional) Number of frames on this slide, 1 is the default and indicates no animations.
-   `objects`: (array) The slide objects on this slide, see Slide Objects below.

#### Slide Objects

Most slide objects are graphical objects of one of the pre-defined types. All graphical objects, like GUI widgets, have a bounding box rectangle which determines their position and size. Objects choose which frames they appear on.

In the file format, slide objects are JSON objects with the following basic properties:

-   `type`: (string enum) Specifies the type of the slide object and what other properties the object may have, see below.
-   `rect`: (4-element array of integers: `[left, top, width, height]`, optional) Specifies the bounding box of the object. Is mandatory for most types.
-   `frames`: (array of integers) Specifies on which frames this object is visible. The first frame is frame 0. A frame's number might exceed the number of frames in the slide, that just means that that frame specification is disregarded.
-   `role`: (string, optional) Specifies a semantic role of this object in the slide. The only currently recognized role is `title`. If an object with this role has extractable text (for example, if it is a text object itself), that text will be used as the slide title if there is no slide title provided with the slide itself.
-   `templates`: (array of strings, optional) IDs of templates to apply to this object.
-   `color`: (string, a LibGUI-recognized color specification such as a hex color, optional) Foreground color for this object. This property applies to many graphical objects who have one foreground color, such as text or geometric primitives.

The following types with their own special properties exist:

-   `text`: A label-like text object. The additional properties are:
    -   `text`: (string) The text being drawn.
    -   `font-size`: (integer) Font size of the text, in points relative to the relative pixel units.
    -   `font-weight`: (string enum, see GML) Font weight of the text.
    -   `font`: (string) Font of the text, must be system-wide accessible.
    -   `text-alignment`: (string enum, see GML) Alignment of the text within the bounding box.
-   `image`: An external image. The additional properties are:
    -   `path`: (string) Path to the image file. This path may be relative, then it is relative to the presenter file. The image format may be any of the image formats supported by SerenityOS.
    -   `scaling`: (string enum, optional) Controls how the image is scaled:
        -   `fit-smallest`: (default) Fit the image into the bounding box, preserving its aspect ratio.
        -   `stretch`: Match the bounding box in width and height exactly; this will change the image's aspect ratio if the aspect ratio of the bounding box is not exactly the same.
        -   `fit-largest`: Make the image fill the bounding box, preserving its aspect ratio. This means that the image will be cut off on the top and bottom or left and right, depending on which dimension is "too large". This is a useful setting for background images.

## See also

-   [Presenter(1)](help://man/1/Applications/Presenter)
