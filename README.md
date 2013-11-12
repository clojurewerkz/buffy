# Buffy, the byte buffer slayer

Buffy is a Clojure library to work with Binary Data, write complete binary protocol implementations
in clojure, store your complex data structures in an off-heap chache, read binary files and do
everything you would usually do `ByteBuffer`.

## Main features

  * partial deserialization (read and deserialise parts of a byte buffer)
  * named access (access parts of your buffer by names)
  * composing/decomposing from key/value pairs
  * pretty hexdump
  * many useful default types that you can combine and extend easily

### The Most Recent Release

With Leiningen:

```clj
[clojurewerkz/buffy "0.1.0-SNAPSHOT"]
```

With Maven:

```xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>buffy</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Usage

Refer Buffy into your project: `clojurewerkz.buffy.core`.
Buffy creates all buffers based on a spec you specify. Spec consists of one or many data types,
for example:

```clj
{:my-field-1 (int32-type)
 :my-field-2 (string-type 10)}
```

Is a specification for a buffer containing 2 fields, one 4 bytes long and second one 14:

```
0            4                         14
+------------+-------------------------+
| my-field-1 |         my-field-2      |
|    (int)   |        (10 string)      |
+------------+-------------------------+
```

Now, you can use this specification to create a byte buffer:

```clj
(compose-buffer {:my-field-1 (int32-type) :my-field-2 (string-type 10)}
```

Keys (`:my-field-1`, `:my-field-2`) aren't a part of byte buffer. They're stored _only_ in a spec.
If you're transferring a byte buffer over a network, receiving part should implement it's own parsing.

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
