self.onmessage = ({ data }) => {
    self.postMessage(`Worker responding to: ${data}`);
};
