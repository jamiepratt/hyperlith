(ns hyperlith.impl.gzip
  (:import (java.io ByteArrayOutputStream)
           (java.util.zip GZIPOutputStream)))

(defn gzip
  [string]
  (with-open [out  (ByteArrayOutputStream/new)
              gzip (GZIPOutputStream/new out)]
    (doto gzip
      (.write  (String/.getBytes string))
      (.finish))
    (.toByteArray out)))
