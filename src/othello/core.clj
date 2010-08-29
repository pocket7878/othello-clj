(ns othello.core
  (:gen-class)
  (:import (java.awt Graphics Color Dimension)
     (javax.swing JPanel JFrame ImageIcon)
     (java.awt.event MouseListener))
  (:use clojure.contrib.import-static
        clojure.contrib.trace
     [clojure.contrib.seq-utils]))

;; 定数を取得
(import-static javax.swing.JFrame EXIT_ON_CLOSE)

;; プログラム内の定数を設定
(def width 8)
(def height 8)
(def cell-size 50)

;; 画像の読み込み
(def gray_cell_img (.getImage (ImageIcon. "src/othello/img/gray_panel.png")))
(def blue_cell_img (.getImage (ImageIcon. "src/othello/img/blue_panel.png")))
(def pink_cell_img (.getImage (ImageIcon. "src/othello/img/pink_panel.png")))
(def hint_cell_img (.getImage (ImageIcon. "src/othello/img/hint_panel.png")))

;;; プログラム内で利用する関数

;;セルの個数を数える
(defn count-cell [cell db]
  (count (filter (fn [x] (= cell x)) @db)))

;;ボードがすべて埋まっているかどうかを判断する
(defn filled? [db]
  (zero? (count-cell 0 db)))

;;ユーザのセルの数を数える
(defn count-user-cell [db]
  (count-cell 1 db))

;;AIのセルの数を調べる
(defn count-ai-cell [db]
  (count-cell 2 db))

;;空のボードを作成
(defn create-blank-board [] (ref (vec (repeat (* width height) 0))))
;;XY座標からboard上の番号に変換
(defn point-to-cell [x y]
  (+ (* y height) x))
;;セルに値を設定
(defn set-cell! [x y var db]
  (dosync (ref-set db (assoc @db (point-to-cell x y) var))))
;;初期のマスを配置
(defn put-init-stone [db]
  (set-cell! 3 3 1 db)
  (set-cell! 4 4 1 db)
  (set-cell! 3 4 2 db)
  (set-cell! 4 3 2 db)
  db)
(defn create-board []
  (put-init-stone (create-blank-board)))
;;ボードをリセット
(defn reset-board [db]
  (dosync (ref-set db (create-board))))

;;board上の番号からXY座標に変換する
(defn cell-to-point [num]
  {:x (rem num width)
   :y (quot num height)})

;;XY座標からスクリーン上の座標に変更
(defn point-to-screen [pt]
  (map #(* cell-size %)
       [ (pt :x) (pt :y) 1 1]))


;;セルの値を取得
(defn get-cell [x y db]
  (@db (point-to-cell x y)))

;;Cellを塗りつぶす
(defn fill-cell [g pt color]
  (let [[x y width height] (point-to-screen pt)]
    (.setColor g color)
    (.fillRect g x y width height)))

;; スクリーン上の点がどのセルに属するのかを判断
(defn dir-to-cell [x y]
  {:x (quot x cell-size)
   :y (quot y cell-size)})

(defn fill-cell-with-image [g pt image]
  (let [[x y width height] (point-to-screen pt)]
    (.drawImage g image x y width height nil)))

(defn draw-board [g db]
  (doseq [[idx elt] (indexed @db)]
    (cond (= elt 0) (fill-cell-with-image g (cell-to-point idx) gray_cell_img)
      (= elt 1) (fill-cell-with-image g (cell-to-point idx) pink_cell_img)
      (= elt 2) (fill-cell-with-image g (cell-to-point idx) blue_cell_img)
      (= elt 3) (fill-cell-with-image g (cell-to-point idx) hint_cell_img))))

;;セルが盤面内かどうか
(defn in-boardp [x y]
  (when (and (>= x 0) (< x width)
             (>= y 0) (< y height)) true))
;;そこに石を打つ事が可能かどうかを判断する
(defn can-put-downp 
  ([x y var db]
    (cond (not (in-boardp x y)) nil
          (not (= (get-cell x y db) 0)) nil
          (or (can-put-downp x y var 1 0 db)
              (can-put-downp x y var 0 1 db)
              (can-put-downp x y var -1 0 db)
              (can-put-downp x y var 0 -1 db)
              (can-put-downp x y var 1 1 db)
              (can-put-downp x y var -1 -1 db)
              (can-put-downp x y var 1 -1 db)
              (can-put-downp x y var -1 1 db)) true
          true nil))
  ([x y var vecX vecY db]
    (cond 
      ;;盤面外だと打てない
      (not (in-boardp (+ vecX x) (+ vecY y))) nil
      ;;隣が自分とおなじ石だとうてない
      (= var (get-cell (+ vecX x) (+ vecY y) db)) nil
      ;;隣が空白でも打てない
      (= 0   (get-cell (+ vecX x) (+ vecY y) db)) nil
      true
      (loop [X (+ vecX vecX x) Y (+ vecY vecY y)]
        (cond 
          ;;盤面外だと打てない
          (not (in-boardp X Y)) nil  
          ;;一つも挟めないとうてない
          (= 0 (get-cell X Y db)) nil
          ;;自分とおなじ石があれば挟めるので打てる
          (= var (get-cell X Y db)) true
          ;;相手の石しかなければ盤面外にでるのでnil
          true (recur (+ vecX X) (+ vecY 1)))))))

(defn game-panel [db]
  (proxy [JPanel MouseListener] []
    (paintComponent [g]
                    (proxy-super paintComponent g)
                    (draw-board g db))
    (mouseClicked [e]
                  (when (can-put-downp ((dir-to-cell (.getX e) (.getY e)) :x)
                                       ((dir-to-cell (.getY e) (.getY e)) :y) 1 db)
                    (set-cell! ((dir-to-cell (.getX e) (.getY e)) :x)
                               ((dir-to-cell (.getX e) (.getY e)) :y)
                               1
                               db)
                    (.repaint this)))
    (mousePressed [e])
    (mouseReleased [e])
    (mouseEntered [e])
    (mouseExited [e])
    (getPreferredSize [] (Dimension. 400 400))))

(defn -main []
  (let [board (create-board)
        frame (JFrame. "Othello")
        panel (game-panel board)]
    (doto panel
      (.addMouseListener panel))
    (doto frame
      (.add panel)
      (.pack)
      (.setDefaultCloseOperation EXIT_ON_CLOSE)
      (.setVisible true))
    [frame panel board]))
