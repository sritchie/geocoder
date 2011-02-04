;; For a little more about the zipper data structure, take a look:
;; http://en.wikibooks.org/wiki/Clojure_Programming/Examples/API_Examples/Advanced_Data_Structures#zipper

(ns geocoder.core
  (:use cascalog.api)
  (:require (clojure.contrib.http [agent :as h])
            (clojure [xml :as x] [zip :as z])
            (clojure.contrib.zip-filter [xml :as zf]))
  (:gen-class))

(defn coord-piece
  "Generates a function that extracts either lat or lng from a
  supplied xml zipper."
  [lat-or-lng]
  (fn [zipper]
    (first (zf/xml-> zipper :result :geometry :location lat-or-lng zf/text))))

(def lat (coord-piece :lat))
(def lng (coord-piece :lng))

(defn parse
  "Parses the xml at a given address into zipper format, and returns a
  [lat lng] vector. For bad addresses, we'll get [nil, nil]."
  [addr]
  (let [xml-zipper (z/xml-zip (x/parse addr))]
    [(lat xml-zipper) (lng xml-zipper)]))

(defn geocode
  "Returns the lat and long for a given address line."
  [line]
  (parse (str "http://maps.google.com/maps/api/geocode/xml?address="
              line
              "&sensor=false")))

(defn process-addresses
  "Cascalog query to parse all files in a given directory, and print
  (lat, lng, address) to stdout. All bad addresses are stacked up at
  the beginning."
  [output-tap csv-dir]
  (let [source (hfs-textline csv-dir)]
    (?<- output-tap [!!lat !!lng ?line]
         (source ?line)
         (geocode ?line :> !!lat !!lng))))

(defn -main [csv-dir output-dir]
  (process-addresses (hfs-textline output-dir) csv-dir))