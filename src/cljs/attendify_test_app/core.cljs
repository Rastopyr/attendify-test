(ns attendify-test-app.core
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [testdouble.cljs.csv :as csv]))

; (enable-console-print!)

(defonce app-state (atom {:labels []
                          :data []}))

; `read-csv` and `handle-file-change` taken form StakOverflow or similar
;resources and adapt to this task

(defn read-csv [data & options]
  (mapv #(str/split % #",")
       (str/split data #"\n")))

(defn process-upload [fileData]
  (let [parsed (read-csv fileData)]
    (swap! app-state assoc :labels (apply vec (take 1 parsed)))
    (swap! app-state assoc :data (vec (drop 1 parsed)))))

(defn handle-file-change [ev]
  (let [file (nth (-> ev .-target .-files array-seq) 0)
        reader (js/FileReader.)
        onload #(process-upload (-> % .-target .-result))]
    (aset reader "onload" onload)
    (when file
      (.readAsText reader file))))

(rum/defc aggregationView
  "
    Aggregation results
  "
  [incomeIndex]
  (let [values (map #(js/Number (nth % incomeIndex)) (get @app-state :data))
        sum (apply + values)
        avg (/ sum (count values))]
    [:div
      [:div (str "Sum:" sum)]
      [:div (str "Avg:" avg)]]))



(rum/defc uploadInput
  "
    Input for file uploading
  "
  []
  [:div { :class "input-wrapper"}
    [:input {
             :type "file"
             :accept ".csv"
             :on-change handle-file-change}]])

(rum/defc inputView < rum/reactive
  "
    View of input. Update value in `:data` cell when input value did change.
  "
  [ref]
  [:div {:class "input"}
   [:input {:type "text"
            :value (rum/react ref)
            :on-change #(reset! ref (.. %1 -target -value))}]])

(rum/defc inputWrapper < rum/reactive
  "
    Wrapper of input. Create cursor to
  "
  [labelIndex rowIndex]
  [:div { :class "row"}
    (inputView (rum/cursor-in app-state [:data rowIndex labelIndex]))])

(rum/defc rowView
  "
    Row container. Static component.
    Render count of columns equal of labels count
  "
  [index row]
  [:div {:class "row" :key index }
    (let [labels (get @app-state :labels)]
      (map-indexed #(inputWrapper %1 index) labels))])

(rum/defc tableView < rum/reactive
  "
    Component of table. Render rows of table.
    Rerender when `:data` in `app-state` did change
  "
  []
  (let [data (rum/cursor-in app-state [:data])]
    (when (> (count (rum/react data)) 0)
      [:div
        [:div [:h2 "Table"]]
        (map-indexed rowView (rum/react data))
        (aggregationView 1)])))

(rum/defc container
  "
    Container of app
  "
  []
  [:div {:class "container"}
   (uploadInput)
   [:div {:class "table-wrapper"}
    (tableView)]])

(defn render []
  (rum/mount (container) (. js/document (getElementById "app"))))
