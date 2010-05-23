(ns othello.core
  (:import (java.awt Graphics)
	   (javax.swing JPanel JFrame ImageIcon)
	   (java.awt.event ActionListener MouseListener))
  (:use clojure.contrib.import-static
	[clojure.contrib.seq-utils]))
;; 定数を取得
(import-static javax.swing.JFrame EXIT_ON_CLOSE)
;; プログラム内の定数を設定
(def width 8)
(def height 8)
(def cell-size 50)

;; 画像の読み込み
(def blue_cell_img (ImageIcon. "./img/blue_panel.png"))
(def pink_cell_img (ImageIcon. "./img/pink_panel.png"))
(def hint_cell_img (ImageIcon. "./img/hint_panel.png"))

;;; プログラム内で利用する関数

;;セルの個数を数える
(defn count-cell [cell db]
  (count (filter (fn [x] (= cell x)) db)))

;;ボードがすべて埋まっているかどうかを判断する
(defn filled? [db]
  (zero? (count-cell 0 db)))

;;ユーザのセルの数を数える
(defn count-user-cell [db]
  (count-cell 1 db))
;;AIのセルの数を調べる
(defn count-ai-cell [db]
  (count-cell 2 db))

;;ボードを作成
(def board (ref (vec (repeat (* width height) 0))))

;;ボードをリセット
(defn reset-board [db]
  (dosync (ref-set db (vec (repeat (* width height) 0)))))

;;XY座標からboard上の番号に変換
(defn point-to-cell [x y]
  (+ (* y height) x))

;;board上の番号からXY座標に変換する
(defn cell-to-point [num]
  {:x (- (rem num (- width 1)) 1) 
   :y (quot num (- height 1))})

;;XY座標からスクリーン上の座標に変更
(defn point-to-screen [pt]
  (map #(* cell-size %)
       [ (pt :x) (pt :y) 1 1]))

;;セルに値を設定
(defn set-cell! [x y var db]
  (dosync (ref-set db (assoc @db (point-to-cell x y) var))))

;;Boardを描画
(defn paint-board [db]
  (for [[idx elt] (indexed @db)]
    (if (ed

