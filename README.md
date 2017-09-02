# Buffy, The Byte Buffer Slayer

Buffy is a Clojure library for working with binary data, writing
complete binary protocol implementations in Clojure, storing complex
data structures in an off-heap cache, reading binary files and doing
everything you would usually do with `ByteBuffer`.

## Main features

  * partial deserialization (read and deserialize parts of a byte buffer)
  * named access (access parts of your buffer by names)
  * composing/decomposing from key/value pairs
  * pretty hexdump
  * many useful default types that you can combine and extend easily


## Project Maturity

Buffy is a young project. The API is fairly stable and the project has reached
1.0 in December 2014.


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
[clojurewerkz/buffy "1.1.0"]
```

With Maven:

```xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>buffy</artifactId>
  <version>1.1.0</version>
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

```clojure
(spec :my-field-1 (int32-type)
      :my-field-2 (string-type 10))
```

The spec can be a map (e.g. `array-map`) or a vector of vectors.
Avoid using hash maps, since they are unordered.

Below is a specification for a buffer containing 2 fields, one 4 bytes
long and second one 10:

```
0            4                         14
+------------+-------------------------+
| my-field-1 |         my-field-2      |
|    (int)   |        (10 string)      |
+------------+-------------------------+
```

Now you can use this specification to create a byte buffer:

```clojure
(compose-buffer (spec :my-field-1 (int32-type) :my-field-2 (string-type 10)))
;= a byte buffer
```

Note that they keys (`:my-field-1`, `:my-field-2`) are not part of the
byte buffer (not serialized in the data). If you're transferring a
byte buffer over a network, the receiving end should be able to
deserialize it.

### Accessing Fields In The Payload

Use `get-field` and `set-field` to access individual fields of the
payload.

Here's an example:

```clojure
(ns my-binary-project.core
  (:require [clojurewerkz.buffy.core :refer :all]))

(let [s    (spec :int-field (int32-type)
                 :string-field (string-type 10))
      buf  (compose-buffer s)]

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

```clojure

(let [s     (spec :first-field (int32-type)
                  :second-field (string-type 10)
                  :third-field (boolean-type))
      buf  (compose-buffer spec)]

  (compose buf {:first-field 101
                :second-field "string"
                :third-field true})

  (decompose buf)
  ;; => {:third-field true :second-field "string" :first-field 101}
)
```


## Data Types

Built-in data types are:

### Primitive types

  * `int32-type`: 32 bit integer
  * `boolean-type`: boolean (1 byte)
  * `byte-type`: a single byte
  * `short-type`: 16 bit integer
  * `medium-type`: 24 bit integer
  * `float-type`: 32 bit floating point
  * `long-type`: 64 bit integer

### Arbitrary-length types

  * `string-type` - arbitrary size `string` (you define the length).

In order to construct a `string-type`, specify its length:

```clojure
(string-type 15)
```

  * `bytes-type` - arbitrary size `byte-array` (you define length yourself)

Same is true for `BytesType`, when constructing it, just pass a number of bytes it should
contain:

```clojure
(bytes-type 25)
```

### Bit type

Bit type is `n` bits long sequence of bits that are turned either on or off, for example,

```clojure
[true true false false
 false false false false
 false false false false
 false false false false
 false false false false
 false false false false
 false false false false
 false false false false]
```

Translates to binary

```
0000 0000 0000 00011
```

Which translates to decimal `3`, that is stored in a 4-bits integer field.

There are some helper functions, such as:

```clojure
(clojurewerkz.buffy.util/bits-on-at [0 1 2])

[true true true false
 false false false false
 false false false false
 false false false false
 false false false false
 false false false false
 false false false false
 false false false false]
```

Or an inverse of it, `clojurewerkz.buffy.util/bits-off-at`.

Also, `on-bits-indexes` that returns positions at which bits are set, and
`off-bits-indexes` that returns positions at which bits are cleared.

In order to use bit type, you need to give it a 32-items long sequence of
truthy or falsy falues:

```clojure
(let [s (spec :first-field (bit-type 4) ;; Bit field that fills 4 bytes
              :second-field (string-type 10))
      buf (compose-buffer s)]
  (set-field b :first-field [true  true  false false
                             false false false false
                             false false false false
                             false false false false
                             false false false false
                             false false false false
                             false false false false
                             false false false false]))
```

### Complex (Composite) Types

Composite types combine multiple primitive types.

`composite-type` produces a slice of a buffer. In the byte representation, no
paddings or offsets are added. All parts are written to the buffer
sequentially:

Here's what composite type consisting of `int` and 10 characters long
`string` would look like:

```clojure
(composite-type (int32-type) (string-type 10))
```

`repeated-type` repeats a type one or more times.  Repeated types are
used when you need to have many fields of the same size:

```clojure
(repeated-type (string-type 10) 5)
```

will produce a type consisting of 5 `strings` of length 10.

It's possible to combine `repeated-type` and `composite-type`:

```clojure
(repeated-type (composite-type (int32-type) (string-type 10)) 5)
```

Which will construct a type consisting of `int`/`string` chunks
repeated 5 times.

 `enum-type` produces a mapping between human-readable values and
 their internal binary representation.

Consider a binary protocol where the `STARTUP` verb is encoded as a
long value of `0x01` and the `QUERY` verb is encoded as `0x07`:

```clojure
(enum-type (long-type) {:STARTUP 0x02 :QUERY 0x07})
```

With this enum type, it is possible to set a field using `:STARTUP`
and `:QUERY` keywords:

```clojure
(set-field buffer :payload-type :STARTUP)
```

When reading a field, its symbolic representation is returned:

```clojure
(get-field buffer :payload-type)
;; => :QUERY
```

## Buffer types

Currently, Buffy supports `direct`, `heap` and `wrapped` buffers.
In order to create a heap buffer:

```clojure
(def my-spec (spec :first-field (int32-type)
                   :second-field (string-type 10)))
(compose-buffer my-spec :buffer-type :heap)
```

For off-heap (direct) buffer:

```clojure
(def my-spec (spec :first-field (int32-type)
                   :second-field (string-type 10)))
(compose-buffer my-spec :buffer-type :direct)
```

And for wrapped buffer (that wraps the given byte array,
`j.nio.ByteBuffer` or netty `ByteBuf`):

```clojure
(def my-spec (spec :first-field (int32-type)
                   :second-field (string-type 10)))
(compose-buffer my-spec :orig-buffer (java.nio.ByteBuffer/allocate 14))
```

## Dynamic Frames

If you're working with sophisticated protocols, more often than not you can't know
the buffer size before you construct an entire type. One of the most primitive examples
is the `netstrings` protocol, that consists of

```clojure
(short-type) ;; Identifies the length of string
(string-type 10) ;; Identifies the string itself
```

Problem with construction of such type lays in the fact that you can't construct a buffer
before you know the value of the string itself. Buffy helps you here, too. This feature
is called dynamic frame. In order to construct a dynamic frame, you should create an
encoder and decoder. Let's take a closer look at netstrings protocol implementation:

First, encoder:

```clojure
(frame-encoder [value]
               ;; Name     ;; Child frame or type      ;; Dynamic value
               length      (short-type)                (count value)
               string      (string-type (count value)) value)
```

Here, in a binding you have a `value`. `length` part of the frame is a `short-type` that
holds a length of the string, you specify this value through `(count value)`.

Next off, the `string` itself, that is a `string-type` and holds a `value` itself.

Decoder is written in a same manner:

```clojure
(frame-decoder [buffer offset]
               length (short-type)
               string (string-type (read length buffer offset)))
```

Since values are not decoded by that time just yet, and you may need access to an entire
buffer in order to read a certain field's value, you specify only types and have a possibility
of "look-behind", using already constructed types.

So, `string` type is constructed by reading off the `length` as a first field of the frame.

An entire frame would look as follows:

```clojure
(def dynamic-string-payload
  (dynamic-buffer
   (frame-type
    (frame-encoder [value]
                   length (short-type) (count value)
                   string (string-type (count value)) value)
    (frame-decoder [buffer offset]
                   length (short-type)
                   string (string-type (read length buffer offset)))
    second ;; Value Formatter
    )))
```

`second` here is just a value formatter. When we read off the value from the buffer, we see the
`short` as well as `string`, but it's just a helper for correct decomposition, therefore
we should discard it and take only the second value, which is a string itself.

In order to compose/decompose it, you should use `compose` and `decompose` functions:

```clojure
(compose dynamic-string-payload ["super-duper-random-string" "long-ans-senseless-stringyoyoyo"])
```

This one will return a buffer. Same with `decompose`, that receives dynamic buffer and a value,
and returns deserialized value.

You can go ahead and create even more complicated patterns. For example, you can construct
a map of strings (as in Cassandra binary CQL protocol), where the map is specified by

```
<short>|(repeated <string>|<string>)
```

Where each `<string>` is actually

```
<short>|<string itself>
```

It's implementation is a little bit more complex, but still reasonably simple. First, we
define a dynamic string frame in the same manner as we made with `netstrings`:

```clojure
(def dynamic-string
  (frame-type
   (frame-encoder [value]
                  length (short-type) (count value)
                  string (string-type (count value))
                  value)
   (frame-decoder [buffer offset]
                  length (short-type)
                  string (string-type (read length buffer offset)))
   second))
```

Next off, key-value pairs. Each one of them is nothing more than a string repeated twice.

```clojure
(def key-value-pair
  (composite-frame
   dynamic-string
   dynamic-string))
```

Next is dynamic map, which is a frame type that holds a `length` which is `short-type` and
`repeated-frame` of `key-value-pairs`:

```clojure
(def dynamic-map
  (frame-type
   (frame-encoder [value]
                  length (short-type) (count value)
                  map    (repeated-frame key-value-pair (count value)) value)
   (frame-decoder [buffer offset]
                  length (short-type)
                  map    (repeated-frame key-value-pair (read length buffer offset)))
   second))
```

Now, our dynamic map is ready for composition and decomposition:

```clojure
(let [dynamic-type (dynamic-buffer dynamic-map)]
  (compose dynamic-type [[["key1" "value1"] ["key1" "value1"] ["key1" "value1"]]]) ;; Returns a constructred buffer

  (-> dynamic-type
      (compose [[["key1" "value1"] ["key1" "value1"] ["key1" "value1"]]])
      decompose) ;; Decomposes it back to the key-value pairs
```

## Working With Bits

In Java, there are no data types for bits, therefore
we've added some wrapper functions for existing types, that may
represent your values as series of 1s and 0es. For example, you can
convert an integer `101` to it's binary representation:

```clojure
(to-bit-map (int32-type) 101)
```

This will return a bitmap of `0000 0000   0000 0000   0000 0000   0110 0101` (represented as
vector of `true` and `false`), which is a binary representation of `101`.

Same way, you can convert a bitmap consisting of `true` and `false`
back to it's actual value with `from-bit-map` function.

## Hex Dump

It is possible to produce a hex-dump of a buffer created with Buffy
using `clojurewerkz.buffy.util/hex-dump`. It will produce the
following representation:

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


## Community

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](http://twitter.com/clojurewerkz) on Twitter.

## Supported Clojure Versions

Buffy requires Clojure 1.4+.

## Continuous Integration Status

[![Continuous Integration status](https://secure.travis-ci.org/clojurewerkz/buffy.png)](http://travis-ci.org/clojurewerkz/buffy)

## Buffy Is a ClojureWerkz Project

Buffy is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with

 * [Langohr](http://clojurerabbitmq.info)
 * [Elastisch](http://clojureelasticsearch.info)
 * [Cassaforte](http://clojurecassandra.info)
 * [Monger](http://clojuremongodb.info)
 * [Titanium](http://titanium.clojurewerkz.org)
 * [Neocons](http://clojureneo4j.info)
 * [Quartzite](http://clojurequartz.info)

and several others.


## Development

Buffy uses [Leiningen 2](http://leiningen.org). Make sure you
have it installed and then run tests against supported Clojure
versions using

    lein all test

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.



## License

Copyright (C) 2013-2016 Alex Petrov, Michael S. Klishin and the ClojureWerkz Team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
