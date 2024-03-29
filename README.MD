# SFTP Extension

## What is this repository for?

 The SFTP Extension allows you to configure Thingworx entities to connect to a remote SFTP (SSH File Transfer Protocol) server for the purposes of managing the file system,  like copying, moving or deleting files, as well as file transfer functionalities, like uploading or downloading files. The extension uses SSH for its underlying transport, so it provides secure authentication and transport. Both password, and key based authentication are supported.

## How do I get set up?

* To build the importable zip extension run the gradle task **packageExtension**. 
* Install the extension archive "zip/sftpExtension.zip" using the Extension import tool from ThingWorx.
* Create Things that inherit the SftpRepositoryTemplate ThingTemplate.
* On the created Things, configure them according to the documentation

## Developing

You can use the gradle tasks *eclipse* or *idea* to generate projects that you can use in your favourite IDE

Please open an issue or contact placatus@iqnox.com for support.
This Extension is provided as-is and without warranty or support. It is not part of the PTC product suite. This project is licensed under the terms of the MIT license.
