(ns hyperlith.impl.util
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)
           (java.util.zip GZIPOutputStream)
           (java.security SecureRandom)
           (java.util Base64 Base64$Encoder)))

;;; - JSON -

(defn json-stream->edn [json]
  (-> json io/reader (json/read {:key-fn keyword})))

;;; - GZIP -

(defn gzip
  [string]
  (with-open [out  (ByteArrayOutputStream/new)
              gzip (GZIPOutputStream/new out)]
    (doto gzip
      (.write  (String/.getBytes string))
      (.finish))
    (.toByteArray out)))

;;; - CRYPTO -

(def ^SecureRandom secure-random
  (SecureRandom/new))

(def ^Base64$Encoder base64-encoder
  (.withoutPadding (Base64/getUrlEncoder)))

(defn random-unguessable-uid
  "URL-safe base64-encoded 160-bit (20 byte) random value. Speed
  is similar random-uuid.
  See: https://neilmadden.blog/2018/08/30/moving-away-from-uuids/"
  []
  (let [buffer (byte-array 20)]
    (.nextBytes secure-random buffer)
    (.encodeToString base64-encoder buffer)))
