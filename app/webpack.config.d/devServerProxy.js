config.devServer = config.devServer || {};

config.devServer.proxy = {
    "/images": {
        target: "https://bigboxcollection.com",
        changeOrigin: true,
        secure: true
    }
};