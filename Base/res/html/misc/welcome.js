document.addEventListener("DOMContentLoaded", function () {
    console.log(
        "Hello from DOMContentLoaded! There should be a message before this from an inline script"
    );
    document.getElementById("ua").innerHTML = navigator.userAgent;
});
