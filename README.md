# ext-package

## What is this?

Given the following:

- You have used [swing-extras](https://github.com/scorbo2/swing-extras/) to create a Java application
- You're using the AppExtension mechanism in swing-extras to allow for dynamically-loaded extensions
- You want to use the UpdateManager in swing-extras to allow for dynamic extension discovery and download

Then you can use `ext-package` to digitally sign your extension jars, and generate the application update
json that will allow your application to discover your extensions, download them, and install them.

## How does it work?

There are two json files that need to be generated:

- the sources json that you ship with your application. This one is very small, and basically just answers the question of "where do I look for extensions?"
- the extension version json that you host somewhere. This one is comprehensive, and answers the question "what extensions are available for which versions of my application?"
 
The `ext-package` application can generate both of these for you, which saves a lot of hand-editing of json.

## Optional - Package signing

Signing your extension jars is optional, but highly recommended. Your application can use the `SignatureUtil` facility
in swing-extras to verify that the extension jar that is downloaded is valid. The following requirements must be
met to use package signing:

- You have generated a public/private key pair
- You have made the public key available on your web host
- Your extension jars are signed with your private key

The `ext-package` application can handle all of this for you (including key pair generation).

## How do I use it?

Fire up the application and walk through the UI:

- Generate a key pair (optional)
- Enumerate and sign your extension jar(s) (signing is optional)
- Generate the application update json (to be hosted on your web server)
- Generate the sources json (to be bundled with your application)
- attach optional screenshots to your extensions

From there, you can upload the output directory to your web host and distribute your application!
Your application will then be able to dynamically discover, download, and install your extensions.

## Can I add new extensions or versions after the application is released?

Yes! That's the whole point! :) By re-running `ext-package` with your new extension(s), or new
extension version(s), you can generate new version json to be uploaded to your web host, and your
already-distributed application can detect the new extension(s) or new version(s) and present them
for download/upgrade. 

## What if I release a new application version?

Your application will be able to detect that it is no longer the latest version, and can notify
the user that an upgrade is available. (Auto-application update not yet supported).

## License

ext-package is made available under the MIT license: https://opensource.org/license/mit

## Revision history

TODO work in progress 2025-10-13
