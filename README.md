# brightspace-adapter


## Installation

You'll need [Leiningen](https://leiningen.org/).

### Build an executable JAR:
```bash
# prod profile:
lein with-profile prod ring uberjar
# no profile:
lein ring uberjar
```
Outputs to target/uberjar/brightspace-adapter-0.1.0-standalone.jar

### Running the JAR
You'll need a config file, `.secrets.edn`, containing the client ID and secret
to be present in the directory you run it from. Client ID and secret can be 
obtained from Brightspace -> settings wheel -> Manage Extensibility -> OAuth2.0

Other configuration options can be set before build time in 
`resources/config.edn`.
#### `.secrets.edn`
```edn
{:oauth {:client-id "<client-id>"
         :client-secret "<client-secret>"}}
```



## Usage
### Running a dev server
```bash
lein ring server-headless
```
When you make a request and code has changed on disk, it will be reloaded in
this mode.

### Setting up your tokens
Go to the HTTPS endpoint of the local server, prefix /brightspace/setup
This will send you to Brightspace to approve the requested API access. You'll
return, assuming the redirect URLs are set appropriately, and the resulting
access/refresh tokens will be saved. You should see a page that says something
like "You're all set".

### Using it
Post a request to /brightspace/user with JSON body containing the parameters
defined on the POST endpoing in core.clj. The application will check with 
Brightspace to determine whether or not the user exists; if so, it does
nothing. If not, it will use the provided attributes to create a new user.


## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
