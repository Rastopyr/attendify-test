(ns attendify-test-app.core
  (:require [clojure.string :as str]
            [rum.core :as rum]
            [testdouble.cljs.csv :as csv]))

(enable-console-print!)

(defonce app-state (atom {:maxFileSize 1024
                          :columns 2
                          :data []
                          :error nil}))

; `read-csv` and `handle-file-change` taken form StakOverflow or similar
;resources and adapt to this task

(defn read-csv [data & options]
  (doall (mapv #(str/split % #",")
          (str/split-lines data))))

(defn isValidStructure? [body]
  (let [labels (apply vec (take 1 body))]
    (when (= ["Company", "Income"] labels) true)))

(defn isValidSize? [file]
  (if (< (-> file .-size) (get @app-state :maxFileSize))
    true))

(defn setError [err]
  (let [errors (get @app-state :errors)]
    (swap! app-state assoc :error err)))

(defn process-upload [fileData]
  (swap! app-state assoc :data fileData :error nil)
  (setError nil))

(defn handle-file-change [ev]
  (let [file (nth (-> ev .-target .-files array-seq) 0)
        reader (js/FileReader.)
        onload #(let [body (read-csv (-> % .-target .-result))]
                  (if (isValidStructure? body)
                    (process-upload body)
                    (setError "File is not valid csv")))]
    (aset reader "onload" onload)
    (if (and file (isValidSize? file))
      (.readAsText reader file)
      (setError "File is too big"))))

(defn exportCsv []
  (let [data (get @app-state :data)]
    (->>
      (csv/write-csv data)
      (str "data:text/csv;charset=utf-8,")
      (js/encodeURI)
      (js/open))))

(rum/defc saveButton
  "
    Export data from browser
  "
  []
  [:button {:onClick #(exportCsv)} "Export data"])

(rum/defc aggregationView
  "
    Aggregation results
  "
  [incomeIndex]
  (let [data (map #(js/Number (nth % incomeIndex)) (get @app-state :data))
        values (drop 1 data)
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

(rum/defc errorLine
  ""
  [err]
  [:li err])

(rum/defc errorsView < rum/reactive
  "List of errors
  "
  []
  (let [error (rum/cursor-in app-state [:error])]
    (when error
      [:div {:class "err"} (str (rum/react error))])))

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
  [:div { :class "column"}
    (inputView (rum/cursor-in app-state [:data rowIndex labelIndex]))])

(rum/defc rowView
  "
    Row container. Static component.
    Render count of columns equal of labels count
  "
  [index row]
  [:div {:class "row" :key index}
    (let [columns (range (get @app-state :columns))]
      (map-indexed #(inputWrapper %1 index) columns))])

(rum/defc tableView < rum/reactive
  "
    Component of table. Render rows of table.
    Rerender when `:data` in `app-state` did change
  "
  []
  (let [data (rum/cursor-in app-state [:data])]
    (when (> (count (rum/react data)) 0)
      [:div { :class "table-container"}
        [:div [:h2 "Table"]]
        [:div { :class "row-container"}
          (map-indexed rowView (rum/react data))
          (aggregationView 1)
          (saveButton)]])))

(rum/defc container
  "
    Container of app
  "
  []
  [:div {:class "container"}
   (uploadInput)
   [:div {:class "table-wrapper"}
    (tableView)
    (errorsView)]])

(defn render []
  (rum/mount (container) (. js/document (getElementById "app"))))
