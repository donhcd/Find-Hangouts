(ns find-hang-outs.core
  (:use funnyplaces.api)               
  (:import [com.javadocmd.simplelatlng LatLng LatLngTool util.LengthUnit]))

(factual! "ylIucvB3r2oIQ3iBjOES4h1mZcNCZyBmQFcYREdj" "5VTCQYxgp5lwj9r1FEHfSg0rUCnTU1nZLDpwMrJX")

(defn hang-out? [hang-out]
  (when *assert*
    (and (coll? hang-out)
         (every? map? hang-out))))

(defn hang-out-dist-tuple? [tuple]
  (when *assert*
    (and (coll? tuple)
         (hang-out? (first tuple))
         (number?  (second tuple)))))

(defn map-to-vec [m] (apply concat (seq m)))

(defn nm-merge "nested map merge" [& mms]
  (apply (partial merge-with nm-merge) mms))

(defn get-distance [p1 p2]
  {:pre  [(map? p1)
          (map? p2)]
   :post [(number? %)]}
  (LatLngTool/distance (p1 :LatLng)
                       (p2 :LatLng)
                       LengthUnit/MILE))

(defn add-pois
  "Takes the maximum total distance to walk, a partially built hang-out collection and
  the amount of walking it currently requires, and a list of next pois that need to be
  added to the hang-out, and returns a vector containing all the different hang-outs
  that can be generated by adding one of the pois to the hang-out."
  [max-dist [hang-out dist-so-far] next-pois]
  (let [last-poi (last hang-out)]
    (filter identity
            (map #(let [new-dist (+ dist-so-far (get-distance last-poi %))]
                    (when (<= new-dist max-dist)
                          [(conj hang-out %) new-dist]))
                 next-pois))))

(defn add-pois-to-hang-outs [max-dist hang-outs next-pois]
  {:pre  [(number? max-dist)
          (coll? hang-outs)
          (every? hang-out-dist-tuple? hang-outs)
          (coll? next-pois)
          (every? map? next-pois)]
   :post [(every? hang-out-dist-tuple? hang-outs)]}
  (apply concat
         (map #(add-pois max-dist % next-pois)
              hang-outs)))

(defn add-LatLngs
  "Adds LatLng objects to each poi-data-entry for comparing distances"
  [poi-data-entries]
  (map #(assoc % :LatLng (LatLng. (% :latitude)
                                  (% :longitude)))
       poi-data-entries))

(defn get-poi-data-entries-helper [main-filter poi-filter]
  {:pre  [(map? main-filter)
          (map? poi-filter)]
   :post [(coll? %)
          (every? map? %)]}
  (->> (nm-merge main-filter poi-filter)
       map-to-vec
       (apply (partial fetch :places))))

(defn get-poi-data-entries [main-filter poi-filters]
  {:pre  [(map? main-filter)
          (coll? poi-filters)
          (every? map? poi-filters)]
   :post [(coll? %)
          (every? coll? %)
          (fn [poi-data] (every? #(every? hang-out? %) poi-data))]}
  (map (comp add-LatLngs
             (partial get-poi-data-entries-helper main-filter))
       poi-filters))

(defn get-init-hang-outs [poi-data]
  (map #(seq [[%] 0])
       (first poi-data)))

(defn add-walks-back
  "adds the distance from the last spot to the first to the distance count"
  [hang-outs max-dist]
  {:pre  [(coll? hang-outs)
          (every? hang-out-dist-tuple? hang-outs)]
   :post [(coll? %)
          (every? hang-out-dist-tuple? %)]}
  (filter #(<= (second %) max-dist)
          (map (fn [[hang-out dist]]
                 [hang-out (+ dist (get-distance (first hang-out) (last hang-out)))])
               hang-outs)))

(defn sort-hang-outs
  "sorts the hang-outs by total walking distance"
  [hang-outs]
  (sort-by #(compare (second %2) (second %))
           hang-outs))
                           
(defn find-hang-outs
  "Example input:
    (find-hang-outs 6
                    {:filters {:country \"US\"
                               :region \"CA\"
                               :locality \"Los Angeles\"}}
                    [{:filters {:name {:$bw \"pizza\"}}}
                     {:filters {:name {:$bw \"karaoke\"}}}"
  [max-total-dist main-filter poi-filters]
  {:pre  [(number? max-total-dist)
          (map? main-filter)
          (coll? poi-filters)
          (every? map? poi-filters)]
   :post [(coll? %)
          (every? hang-out-dist-tuple? %)]}
  (let [poi-data (get-poi-data-entries main-filter poi-filters)]
    (loop [hang-outs   (get-init-hang-outs poi-data)
           poi-data   (next poi-data)]
      (if poi-data
          (recur (add-pois-to-hang-outs max-total-dist hang-outs (first poi-data))
                 (next poi-data))
          (add-walks-back hang-outs max-total-dist)))))
