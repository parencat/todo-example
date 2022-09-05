(ns todo.core
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [ring.adapter.jetty :as jetty]
   [integrant.core :as ig]
   [compojure.core :refer [routes context GET POST PUT DELETE]]
   [ring.middleware.json :as json]
   [ring.middleware.params :as params])
  (:import
   [java.sql Connection]
   [org.eclipse.jetty.server Server]))


(defn execute! [^Connection connection query & {:keys [one?] :or {one? true}}]
  (let [exec-fn (if one? jdbc/execute-one! jdbc/execute!)]
    (exec-fn connection query
             {:return-keys true
              :builder-fn  jdbc.rs/as-unqualified-kebab-maps})))


;; =============================================================================
;; Business logic
;; =============================================================================

(defn get-todos [connection status]
  (let [todos (execute! connection ["SELECT * FROM todos WHERE status = ?;" status] :one? false)]
    {:status 200
     :body   todos}))


(defn get-todo [connection id]
  (let [todo (execute! connection ["SELECT * FROM todos WHERE id = ?;" id])]
    {:status 200
     :body   todo}))


(defn create-todo [connection title]
  (let [todo (execute! connection ["INSERT INTO todos (title) VALUES (?) RETURNING *;" title])]
    {:status 201
     :body   todo}))


(defn update-todo [connection {:keys [id title status]}]
  (let [todo (execute! connection ["UPDATE todos SET title = ?, status = ? WHERE id = ? RETURNING *;" title status id])]
    {:status 200
     :body   todo}))


(defn delete-todo [connection id]
  (execute! connection ["DELETE FROM todos WHERE id = ?;" id])
  {:status 200
   :body   {:deleted id}})


;; =============================================================================
;; Duct components
;; =============================================================================

(defmethod ig/init-key ::connection [_ config]
  (jdbc/get-connection
   {:connection-uri (:database-uri config)
    :dbtype         (:database-type config)}))

(defmethod ig/halt-key! ::connection [_ ^Connection connection]
  (.close connection))


(defmethod ig/init-key ::migrate [_ config]
  (let [^Connection connection (:connection config)
        create-todos-table     "CREATE TABLE IF NOT EXISTS todos (
                                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                                  title TEXT NOT NULL,
                                  status TEXT CHECK(status IN (\"active\", \"done\")) NOT NULL DEFAULT \"active\"
                                );"]
    (jdbc/execute! connection [create-todos-table])))


(defmethod ig/init-key ::handler [_ config]
  (let [connection (:connection config)
        app-routes (routes
                    (context "/todos" []
                      (GET "/" {{:strs [status]} :query-params}
                        (get-todos connection status))

                      (POST "/" {{:strs [title]} :body}
                        (create-todo connection title))

                      (context "/:todo-id" [todo-id]
                        (GET "/" []
                          (get-todo connection todo-id))

                        (PUT "/" {{:strs [title status]} :body}
                          (update-todo connection {:id     todo-id
                                                   :title  title
                                                   :status status}))

                        (DELETE "/" []
                          (delete-todo connection todo-id)))))]
    (-> app-routes
        (params/wrap-params)
        (json/wrap-json-body)
        (json/wrap-json-response))))


(defmethod ig/init-key ::server [_ config]
  (let [handler (:handler config)]
    (jetty/run-jetty handler {:port  (:http-port config)
                              :join? false})))

(defmethod ig/halt-key! ::server [_ ^Server server]
  (.stop server))
