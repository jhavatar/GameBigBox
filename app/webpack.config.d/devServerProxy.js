config.devServer = config.devServer || {};

// webpack-dev-server v5 (shipped with Kotlin 2.2.x) requires an array; v4 used an object keyed by path.
config.devServer.proxy = [
    {
        context: ["/images"],
        target: "https://bigboxcollection.com",
        changeOrigin: true,
        secure: true
    }
];