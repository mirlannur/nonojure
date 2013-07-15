(ns recognition.utils
  (:import [java.awt.image BufferedImage]
           [org.opencv.core Mat MatOfByte CvType Scalar Core Size]
           [hu.kazocsaba.imageviewer ImageViewer ResizeStrategy]
           org.opencv.highgui.Highgui
           javax.imageio.ImageIO
           org.opencv.imgproc.Imgproc))

(clojure.lang.RT/loadLibrary "opencv_java246")

(defn show [mat]
  (letfn [(to-image [mat]
            (let [bytes (MatOfByte.)]
              (Highgui/imencode ".png" mat bytes)
              (-> (.toArray bytes)
                  (java.io.ByteArrayInputStream.)
                  (ImageIO/read))))
          (adapt-size [frame mat]
            (let [w (.width mat)
                  h (.height mat)
                  [s-w s-h] (->> (java.awt.Toolkit/getDefaultToolkit)
                                 .getScreenSize
                                 ((juxt #(.getWidth %) #(.getHeight %)))
                                 (map #(* 0.8 %)))
                  ratio (/ w h)
                  [m-w m-h] (map #(min %1 %2) [w h] [s-w s-h])]
              (if (> m-w (* ratio m-h))
                (.setSize frame (* ratio m-h) m-h)
                (.setSize frame m-w (/ m-w ratio)))))
          (show-image [im]
            (let [viewer (hu.kazocsaba.imageviewer.ImageViewer. im)
                  fr (javax.swing.JFrame. "Image")]
              (doto viewer
                (.setStatusBarVisible true)
                (.setPixelatedZoom true)
                (.setResizeStrategy ResizeStrategy/RESIZE_TO_FIT))
              (adapt-size fr mat)
              (.setLocationRelativeTo fr nil)
              (.. fr getContentPane (add (.getComponent viewer)))
              (.setVisible fr true)
              mat))]
    (show-image (to-image mat))))

(defn to-binary-mat [colls]
  (-> (apply concat colls)
      (->>
       (map #(if (zero? %) 0 -1))
       (into-array Byte/TYPE))
      (MatOfByte.)
      (.reshape 1 (count colls))))

(defn to-vecs [mat]
  (for [i (range (.rows mat))]
    (for [j (range (.cols mat))]
      (-> (.get mat i j) seq first))))

(defn read [file]
  (let [m (Highgui/imread (str "resources/examples/" file))]
    (Imgproc/cvtColor m m Imgproc/COLOR_RGB2GRAY)
    m))

(defn save [mat file]
  (Highgui/imwrite file mat))

(defn invert [mat dst]
  (Core/bitwise_not mat dst)
  dst)

(defn invert! [mat]
  (invert mat mat))

(defn transpose [mat]
  (.t mat))

(defn flip! [mat dir]
  (Core/flip mat mat (case dir
                       :x 1
                       :y 0))
  mat)

(defn clone [mat]
  (.clone mat))

(defn subtract [src1 src2 dst]
  (Core/subtract src1 src2 dst)
  dst)

(defn rotate90 [mat]
  (flip! (transpose mat) :x))

(defn zero-mat? [mat]
  (zero? (Core/countNonZero mat)))

(defn resize! [mat scale]
  (Imgproc/resize mat mat (Size.) scale scale Imgproc/INTER_LINEAR)
  mat)

(defn or [src1 src2 dst]
  (Core/bitwise_or src1 src2 dst)
  dst)

#_(let [m (to-binary-mat [[1 0 0]
                        [1 1 0]
                        [1 1 1]])]
  (show m)
  (show   (rotate90 (rotate90 m)))
  (show m))