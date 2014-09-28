# friendly

Friendly RSS Reader - Clojure Cup 2014
http://friendly.clojurecup.com/

## Usage

- Clone the git repo, copy the file `resources/config.edn.example` to `resources/config.edn`
- Register the application to use OAuth from either Google or Github (check the contents of `resources/config.edn.example`).

To compile:

```
$ lein cljsbuild prod once
$ lein run
```

Then point your browser to http://localhost:3000/

## License

Copyright © 2014 Denis Fuenzalida

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
