(require '[clojure.string :as str])

(def search-box (js/document.getElementById "search"))

(.addEventListener search-box "keyup"
                   (fn [event]
                     (let [search-string (str/lower-case (.-value search-box))
                           {with true
                            without false}
                           (group-by #(-> %
                                          .-innerText
                                          str/lower-case
                                          (str/includes? search-string))
                                     (js/document.getElementsByClassName "episode"))
                           first-episode (first with)]
                       (doseq [el (.querySelectorAll js/document ".video")]
                         (set! (.-innerHTML el) ""))
                       (when first-episode
                         (let [video-id (-> first-episode .-dataset .-videoid)
                               video-div (-> first-episode (.querySelector ".video"))]
                           (println video-id)
                           (set! (.-innerHTML video-div)
                                 (str "<iframe allow=\"autoplay; fullscreen; picture-in-picture\" allowfullscreen=\"allowfullscreen\" frameborder=\"0\" height=\"360\" src=\"https://player.vimeo.com/video/" video-id  "\" width=\"640\"></iframe>"))
))
                       (doseq [el with]
                         (-> el .-classList (.remove "hidden")))
                       (doseq [el without]
                         (-> el .-classList (.add "hidden"))))))
