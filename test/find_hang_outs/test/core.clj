(ns find-hang-outs.test.core
  (:use [find-hang-outs.core] :reload)
  (:use [clojure.test])
  (:use [clojure.contrib.pprint])
  (:import [com.javadocmd.simplelatlng LatLng]))

;trial code: (map (fn [[c d]] (println d "\n" (map :name c))) (find-hang-outs.core/find-hang-out 6 {:filters {:country "US" :region "CA" :locality "Los Angeles"}} [{:filters {:name {:$bw "pizza"}}} {:filters {:name {:$bw "karaoke"}}} {:q "park"}]))

(deftest add-pois-test
  (let [lat-lng-1 (LatLng. 10 10.00)
        lat-lng-2 (LatLng. 10 10)
        lat-lng-3 (LatLng. 100 100)
        poi-1     {:LatLng lat-lng-1}
        poi-2     {:LatLng lat-lng-2}
        poi-3     {:LatLng lat-lng-3}]
    (is (= (add-pois 1
                     [[poi-3 poi-1] 0] ;; a hang-out
                     [poi-3 poi-2 poi-2] ;; next pois
                     )
           [[[{:LatLng lat-lng-3} {:LatLng lat-lng-1} {:LatLng lat-lng-2}] 0]
            [[{:LatLng lat-lng-3} {:LatLng lat-lng-1} {:LatLng lat-lng-2}] 0]]))
    (is (= (add-pois 1 [[poi-3] 0] [poi-1])
           ()))))

(deftest add-pois-to-hang-outs-test
  (let [lat-lng-1 (LatLng. 10 10.00)
        lat-lng-2 (LatLng. 10 10)
        lat-lng-3 (LatLng. 100 100)
        poi-1 {:LatLng lat-lng-1}
        poi-2 {:LatLng lat-lng-2}
        poi-3 {:LatLng lat-lng-3}]
    (is (=  (add-pois-to-hang-outs 1.0                 ; max dist
                                 [[[poi-1 poi-1] 0.0] [[poi-1 poi-2] 1.0] [[poi-1 poi-3] 0.0]] ; hang-outs
                                 [poi-1 poi-2 poi-3] ; next pois
                                 )
           [[[poi-1 poi-1 poi-1] 0.0]
            [[poi-1 poi-1 poi-2] 0.0]
            [[poi-1 poi-2 poi-1] 1.0]
            [[poi-1 poi-2 poi-2] 1.0]
            [[poi-1 poi-3 poi-3] 0.0]]))))

(deftest get-poi-data-entries-helper-test
  (is (get-poi-data-entries-helper {:filters {:country "US" :region "CA" :locality "Los Angeles"}}
                                   {:filters {:name {:$bw "pizza"}}})))
 
(deftest get-poi-data-entries-test
  (is (get-poi-data-entries {:filters {:country "US" :region "CA" :locality "Los Angeles"}}
                            [{:filters {:name {:$bw "pizza"}}}
                             {:filters {:name {:$bw "karaoke"}}}
                             {:q "park"}])))

(deftest find-hang-out-test
  (let [hang-outs (find-hang-outs 5  ; max dist
                                  {:filters {:country "US" :region "CA" :locality "Los Angeles"}} ; main filter
                                  [{:filters {:name {:$bw "pizza"}}}   ; individual filters
                                   {:filters {:name {:$bw "karaoke"}}}
                                   {:q "park"}])]
    (doseq [poi (map :name (ffirst hang-outs))]
      (println poi))
    (println (second (first hang-outs)))))
