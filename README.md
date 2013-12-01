# Buffy, The Byte Buffer Slayer

Buffy is a Clojure library for working with binary data, writing
complete binary protocol implementations in Clojure, storing complex
data structures in an off-heap cache, reading binary files and doing
everything you would usually do with `ByteBuffer`.

## Main features

  * partial deserialization (read and deserialise parts of a byte buffer)
  * named access (access parts of your buffer by names)
  * composing/decomposing from key/value pairs
  * pretty hexdump
  * many useful default types that you can combine and extend easily


## Installation

### Artefacts

Latest artifacts are published to
[Clojars](https://clojars.org/clojurewerkz/buffy) If you are using
Maven, add the following repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

```clj
[clojurewerkz/buffy "0.3.0"]
```

With Maven:

```xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>buffy</artifactId>
  <version>0.3.0</version>
</dependency>
```

## Usage

Require Buffy's main namespace:

``` clojure
(ns my.app
  (:require [clojurewerkz.buffy.core :refer :all]))
```

Buffy creates buffers from a spec you specify. The spec consists of
one or more fields of known data types, for example:

```clj
{:my-field-1 (int32-type)
 :my-field-2 (string-type 10)}
```

Below is a specification for a buffer containing 2 fields, one 4 bytes
long and second one 14:

```
0            4                         14
+------------+-------------------------+
| my-field-1 |         my-field-2      |
|    (int)   |        (10 string)      |
+------------+-------------------------+
```

Now you can use this specification to create a byte buffer:

```clj
(compose-buffer {:my-field-1 (int32-type) :my-field-2 (string-type 10)}
;= a byte buffer
```

Note that they keys (`:my-field-1`, `:my-field-2`) are not part of the
byte buffer (not serialized in the data). If you're transferring a
byte buffer over a network, the receiving end should be able to
deserialize it.

### Accessing parts of payload

You can use `get-field` and `set-field` to access particular fields of the payload.

Let's check out a complete example:

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


### Deserializing complete buffer

You can also serialize and deserialize a complete buffer:

```clj

(let [spec {:first-field (int32-type)
            :second-field (string-type 10)
            :third-field (boolean-type)}
      buf  (compose-buffer spec)]

  (set-fields buf {:first-field 101
                   :second-field "string"
                   :third-field true})

  (decompose buf)
  ;; => {:third-field true :second-field "string" :first-field 101}
)
```


## Data Types

Built-in data types are:

### Primitive types

  * `int32-type` - 4 bytes long `int`
  * `boolean-type` - 1 byte long `boolean`
  * `byte-type` - single byte
  * `short-type` - 2 bytes long `short`
  * `medium-type` - 3 bytes long `int`
  * `float-type` - 4 bytes long `float`
  * `long-type` - 4 bytes long `long`

### Arbitrary-length types

  * `string-type` - arbitrary size `string` (you define length yourself).

In order to construct a `StringType`, specify it's length. For example, if you want your
buffer part to hold string of 15 characters, use:

```clj
(string-type 15)
```

  * `bytes-type` - arbitrary size `byte-array` (you define length yourself)

Same is true for `BytesType`, when constructing it, just pass a number of bytes it should
contain:

```clj
(bytes-type 25)
```

### Complex Types

  * `composite-type` - composite type, may combine any of the forementioned types

`CompositeType` is kind of a sub-buffer. In a byte representation, we do not add any paddings or
offsets, just writing all parts sequentially. It's mostly created for your convenience:

Here's what composite type consisting of `int` and 10 characters long `string` would look like:

```clj
(composite-type (int32-type) (string-type 10))
```

  * `repeated-type` - repeats any type given amount of times

`RepeatedType` is used when you need to have many fields of same size:

```clj
(repeated-type (string-type 10) 5)
```

Will make a type consisting of 5 `strings` of length 10.

You can also do tricks like `repeated-composite-type`:

```clj
(repeated-type (composite-type (int32-type) (string-type 10)) 5)
```

Which will construct a type consisting of `int`/`string` chunks repeated 5 times.

  * `enum-type` - is a mapping between some human-readable values and their internal binary representation.

Consider some Binary Protocol, where `STARTUP` verb, that makes server aware of the message type, startup,
represents a long value of `0x01` and `QUERY` verb, that makes server aware, that message contains a query,
is `0x07`.

This is a common pattern in Binary Protocols, but it's hard to keep all the constants in mind, therefore
it's easier to keep their human-readable representations. For that, you can use `enum-type`:

```clj
(enum-type (long-type) {:STARTUP 0x02 :QUERY 0x07})
```

Now, you can set your field using `:STARTUP` and `:QUERY` symbols:

```clj
(set-field buffer :payload-type :STARTUP)
```

When reading the field (let's say, payload contained binary `0x07`, you'll get it's mapping (`:QUERY`) in
return:

```clj
(get-field buffer :payload-type)
;; => :QUERY
```

## Hex Dump

You can also hex-dump buffers created with Buffy by using `clojurewerkz.buffy.util/hex-dump`.

It will yield something like that:

```
            +--------------------------------------------------+
            | 0  1  2  3  4  5  6  7   8  9  a  b  c  d  e  f  |
 +----------+--------------------------------------------------+------------------+
 | 00000000 | 48 69 65 72 20 69 73 74  20 65 69 6e 20 42 65 69 | Hier ist ein Bei |
 | 00000010 | 73 70 69 65 6c 74 65 78  74 2e 20 44 65 72 20 48 | spieltext. Der H |
 | 00000020 | 65 78 64 75 6d 70 20 69  73 74 20 61 75 66 20 64 | exdump ist auf d |
 | 00000030 | 65 72 20 6c 69 6e 6b 65  6e 20 53 65 69 74 65 20 | er linken Seite  |
 | 00000040 | 7a 75 20 73 65 68 65 6e  2e 20 4e 65 75 65 20 5a | zu sehen. Neue Z |
 | 00000050 | 65 69 6c 65 6e 20 6f 64  65 72 20 41 62 73 c3 a4 | eilen oder Abs.. |
 | 00000060 | 74 7a 65 20 73 69 6e 64  20 64 61 6e 6e 20 61 75 | tze sind dann au |
 | 00000070 | 63 68 20 22 5a 65 69 63  68 65 6e 22 20 6d 69 74 | ch "Zeichen" mit |
 | 00000080 | 20 65 69 6e 65 6d 20 62  65 73 74 69 6d 6d 74 65 |  einem bestimmte |
 | 00000090 | 6e 20 43 6f 64 65 20 28  30 61 29 00 00 00 00 00 | n Code (0a)..... |
 +----------+--------------------------------------------------+------------------+
```

## Project Maturity

Buffy is __very__ young but, even though we have a very good feeling about API, we can't guarantee it won't change in
a meantime. We're very open for discussions and suggestions.

## Community

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](http://twitter.com/clojurewerkz) on Twitter.

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
