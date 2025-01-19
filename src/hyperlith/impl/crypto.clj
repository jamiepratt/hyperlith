(ns hyperlith.impl.crypto
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]
           [java.security SecureRandom]
           [java.math BigInteger]
           [java.util Base64 Base64$Encoder]))

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

(defn secret-key->hmac-md5-keyspec [secret-key]
  (SecretKeySpec/new (String/.getBytes secret-key) "HmacMD5"))

(defn bytes->hex [^byte/1 b]
  (format "%08x" (BigInteger/new 1 b)))

(defn hmac-md5
  "Used for quick stateless csrf token generation."
  [key-spec data]
    (-> (doto (Mac/getInstance "HmacMD5")
          (.init key-spec))
      (.doFinal (String/.getBytes data))
      bytes->hex))
