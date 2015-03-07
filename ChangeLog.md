## Changes between 1.0.0 and 1.1.0

UUID type added. Now you can serialize `java.util.UUID` type:

```
(let [s    (spec :first-field  (uuid-type))
      b    (compose-buffer s)
      uuid (java.util.UUID/randomUUID)]
    (set-field b :first-field uuid)
    (get-field b :first-field))
```
