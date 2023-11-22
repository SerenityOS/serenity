onmessage = evt => {
    postMessage(evt.data, null);
};
postMessage("loaded", null);
