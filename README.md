# Buffy, the byte buffer slayer

Buffer is a Clojure library to work with Binary Data.

## Main features

  * partial deserialization (read and deserialise parts of a byte buffer)
  * named access (access parts of your buffer by names)
  * composing/decomposing from key/value pairs
  * pretty hexdump
  * many useful default types that you can combine and extend easily

## Usage

Refer Buffy into your project: `clojurewerkz.buffy.core`.
Buffy creates all buffers based on a spec you specify.

Built-in types are:

  * `int32-type` - 4 bytes long `int`
  * `boolean-type` - 1 byte long `boolean`
  * `byte-type` - single byte
  * `short-type` - 2 bytes long `short`
  * `medium-type` - 3 bytes long `int`
  * `float-type` - 4 bytes long `float`
  * `long-type` - 4 bytes long `long`
  * `string-type` - arbitrary size `string` (you define length yourself)
  * `bytes-type` - arbitrary size `byte-array` (you define length yourself)

  * `composite-type` - composite type, may combine any of the forementioned types
  * `repeated-type` - repeats any type given amount of times

```clj
(ns my-binary-project.core
  (:require [clojurewerkz.buffy.core :refer :all]))

(let [spec {:int-field (int32-type)
            :string-field (string-type 10)}
      buf  (compose-buffer spec)]

  (set-field buf :int-field 101)
  (get-field buf :int-field)
  ;; => 101

  (set-field buf :string-field "stringie")
  (get-field buf :string-field)
  ;; => "stringie"
  )
```

## Supported Clojure Versions

Buffy requires Clojure 1.4+.

## Buffy Is a ClojureWerkz Project

Machine Head is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with

 * [Monger](http://clojuremongodb.info)
 * [Langohr](http://clojurerabbitmq.info)
 * [Elastisch](http://clojureelasticsearch.info)
 * [Cassaforte](http://clojurecassandra.info)
 * [Titanium](http://titanium.clojurewerkz.org)
 * [Neocons](http://clojureneo4j.info)
 * [Quartzite](http://clojurequartz.info)

and several others.


## Development

Machine Head uses [Leiningen 2](http://leiningen.org). Make sure you
have it installed and then run tests against supported Clojure
versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.



## License

Copyright © 2013 Alex Petrov, Michael S. Klishin and the ClojureWerkz Team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
