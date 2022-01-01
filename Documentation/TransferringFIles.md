# Transferring files from Serenity to your host machine

## Method 1: WebServer
Serenity has a built-in web server which extends to your host machine.

Create a WebServer instance of the home directory:

 ```
 WebServer -p 8000 .
 ```

Then we just open `localhost:8000` on our host machine :^)

![](WebServer_localhost.png)

## Method 2: Mount disk_image

Another way is to mount Serenity's disk_image to your host machine by using the following command on *nix systems:

 ```
 sudo mount disk_image
 ```